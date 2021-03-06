/*
 * Copyright 2011 Jon S Akhtar (Sylvanaar)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.sylvanaar.idea.Lua.lang.psi;

import com.intellij.openapi.application.*;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.*;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.startup.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.*;
import com.intellij.psi.search.*;
import com.intellij.util.*;
import com.intellij.util.concurrency.*;
import com.intellij.util.containers.*;
import com.intellij.util.messages.*;
import com.sylvanaar.idea.Lua.lang.*;
import com.sylvanaar.idea.Lua.lang.psi.expressions.*;
import com.sylvanaar.idea.Lua.lang.psi.resolve.*;
import com.sylvanaar.idea.Lua.options.*;
import com.sylvanaar.idea.Lua.util.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: Jon S Akhtar
 * Date: 7/10/11
 * Time: 6:32 PM
 */
public class LuaPsiManager implements ProjectComponent {
    private static final Logger log = Logger.getInstance("Lua.LuaPsiManger");

//    private static final NotNullLazyKey<LuaPsiManager, Project> INSTANCE_KEY = ServiceManager.createLazyKey(
//            LuaPsiManager.class);

    private Future<Collection<LuaDeclarationExpression>> filteredGlobalsCache = null;
    private final Project project;

    private static final ArrayList<LuaDeclarationExpression> EMPTY_CACHE = new ArrayList<LuaDeclarationExpression>();

    public Collection<LuaDeclarationExpression> getFilteredGlobalsCache() {
        try {
            if (filteredGlobalsCache == null)
                filteredGlobalsCache =
                        ApplicationManager.getApplication().executeOnPooledThread(new GlobalsCacheBuilder(project));

            return filteredGlobalsCache.get(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.info("exception creating globals cache", e);
        } catch (ExecutionException e) {
            log.info("exception creating globals cache", e);
        } catch (TimeoutException e) {
            log.info("The global cache is still processing");
        } catch (NullPointerException e) {
            log.info("Null cache");
        }

        return EMPTY_CACHE;
    }

    private MessageBus myMessageBus;

    public LuaPsiManager(final Project project) {
        log.debug("*** CREATED ***");

        this.project = project;


    }

    private void reset() {
        log.debug("*** RESET ***");
        filteredGlobalsCache = null;
//        inferProjectFiles(project);
    }

    private void init(final Project project) {
        log.debug("*** INIT ***");
        myMessageBus.connect().subscribe(PsiManagerImpl.ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener() {
            @Override
            public void beforePsiChanged(boolean isPhysical) {

            }

            @Override
            public void afterPsiChanged(boolean isPhysical) {
                if (filteredGlobalsCache != null) reset();
            }
        });
        filteredGlobalsCache =
                ApplicationManager.getApplication().executeOnPooledThread(new GlobalsCacheBuilder(project));


        inferAllTheThings(project);
        inferenceQueueProcessor.start();
    }

    private void inferAllTheThings(Project project) {
        if (!LuaApplicationSettings.getInstance().ENABLE_TYPE_INFERENCE) return;
        final ProjectRootManager m = ProjectRootManager.getInstance(project);
        final PsiManager p = PsiManager.getInstance(project);


        ProgressManager.getInstance().run(new MyBackgroundableInferencer(project, m, p));
    }

    private void inferProjectFiles(Project project) {
        if (!LuaApplicationSettings.getInstance().ENABLE_TYPE_INFERENCE) return;
        final ProjectRootManager m = ProjectRootManager.getInstance(project);
        final PsiManager p = PsiManager.getInstance(project);

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                final PathsList pathsList = m.orderEntries().withoutSdk().withoutLibraries().sources().getPathsList();
                final List<VirtualFile> virtualFiles = pathsList.getVirtualFiles();

                for (VirtualFile file : virtualFiles) {
                    log.debug(file.getName());
                }
            }
        });
    }


    private QueueProcessor<InferenceCapable> inferenceQueueProcessor;

    private Set<InferenceCapable> work;


    public void queueInferences(InferenceCapable inference) {
        if (!LuaApplicationSettings.getInstance().ENABLE_TYPE_INFERENCE) return;

        synchronized (work) {
            if (work.contains(inference)) {
                log.debug("Already processing " + inference);
                return;
            }
            inferenceQueueProcessor.add(inference);
        }
    }

    public void queueInferences(Collection<InferenceCapable> inferences) {
        if (!LuaApplicationSettings.getInstance().ENABLE_TYPE_INFERENCE) return;

        synchronized (work) {
            for (InferenceCapable item : inferences) {
                if (work.contains(item)) {
                    log.debug("Already processing " + inferences);
                    continue;
                }
                inferenceQueueProcessor.add(item);
            }
        }
    }

    public static LuaPsiManager getInstance(Project project) {
        return project.getComponent(LuaPsiManager.class);
//        return INSTANCE_KEY.getValue(project);
    }

    @Override
    public void projectOpened() {
        work = new ArrayListSet<InferenceCapable>();
        myMessageBus = MessageBusFactory.newMessageBus(this);

        inferenceQueueProcessor =
                new QueueProcessor<InferenceCapable>(new InferenceQueue(project), project.getDisposed(), false);

       // DumbService.getInstance(project).runWhenSmart(new InitRunnable());

        StartupManager.getInstance(project).runWhenProjectIsInitialized(new InitRunnable());
    }

    @Override
    public void projectClosed() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initComponent() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void disposeComponent() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "Lua.PsiManager";
    }

    static class GlobalsCacheBuilder implements Callable<Collection<LuaDeclarationExpression>> {
        private final Project project;

        public GlobalsCacheBuilder(Project project) {
            this.project = project;
        }

        @Override
        public Collection<LuaDeclarationExpression> call() throws Exception {
            DumbService.getInstance(project).waitForSmartMode();
            return ApplicationManager.getApplication()
                                     .runReadAction(new Computable<Collection<LuaDeclarationExpression>>() {

                                         @Override
                                         public Collection<LuaDeclarationExpression> compute() {
                                             return ResolveUtil.getFilteredGlobals(project,
                                                     new ProjectAndLibrariesScope(project));
                                         }
                                     });
        }
    }

    private class InferenceQueue implements Consumer<InferenceCapable> {
        private final Project project;

        public InferenceQueue(Project project) {
            assert project != null : "Project is null";

            this.project = project;
        }

        @Override
        public void consume(final InferenceCapable element) {
            ProgressManager.checkCanceled();

            if (project.isDisposed())
                return;
            if (DumbService.isDumb(project)) {
                log.debug("inference q not ready");
                queueInferences(element);
                return;
            }

            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!element.isValid()) {
                            log.debug("invalid element ");
                            return;
                        }
                        log.debug("inference: " + element.toString());


                        element.inferTypes();
                    } finally {
                        synchronized (work) {
                            work.remove(element);
                        }
                    }

                }
            });
        }
    }

    private class OrderEntryProcessor implements Processor<OrderEntry> {
        private final PsiManager p;
        final boolean projectOnly;

        public OrderEntryProcessor(PsiManager p, boolean projectOnly) {
            this.p = p;
            this.projectOnly = projectOnly;
        }
        public OrderEntryProcessor(PsiManager p) {
            this(p, false);
        }

        @Override
        public boolean process(OrderEntry orderEntry) {
            log.debug("process " + orderEntry.getPresentableName());
            final List<InferenceCapable> files = new ArrayList<InferenceCapable>();
            for (final VirtualFile f : orderEntry.getFiles(OrderRootType.CLASSES)) {
                log.debug("process class " + f.getName());
                processRoot(files, f);
            }
            if (orderEntry instanceof ModuleSourceOrderEntry)
                for (final VirtualFile f : ((ModuleSourceOrderEntry) orderEntry).getRootModel().getContentRoots()) {
                    log.debug("process source" + f.getName());
                    processRoot(files, f);
                }

            queueInferences(files);
            log.debug("order entries processed");
            return true;
        }

        private void processRoot(final List<InferenceCapable> files, VirtualFile f) {
            LuaFileUtil.iterateRecursively(f, new ContentIterator() {
                @Override
                public boolean processFile(VirtualFile fileOrDir) {
                    ProgressManager.checkCanceled();

                    log.debug("process " + fileOrDir.getName());
                    if (fileOrDir.isDirectory()) return true;

                    final FileViewProvider viewProvider = p.findViewProvider(fileOrDir);
                    if (viewProvider == null) return true;

                    final PsiFile psiFile = viewProvider.getPsi(viewProvider.getBaseLanguage());
                    if (!(psiFile instanceof InferenceCapable)) return true;

                    log.debug("forcing inference for: " + fileOrDir.getName());

                    files.add((InferenceCapable) psiFile);

                    return true;
                }
            });
        }
    }

    private class InitRunnable implements Runnable {
        @Override
        public void run() {
            final DumbService dumbService = DumbService.getInstance(project);
            if (dumbService.isDumb())
                dumbService.runWhenSmart(new InitRunnable());
            else
                init(project);
        }
    }

    private class MyBackgroundableInferencer extends Task.Backgroundable {

        private final ProjectRootManager m;
        private final PsiManager p;

        public MyBackgroundableInferencer(Project project, ProjectRootManager m, PsiManager p) {
            super(project, "Infering and Propagating Lua Types", true, PerformInBackgroundOption.DEAF);
            this.m = m;
            this.p = p;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    m.orderEntries().forEach(new OrderEntryProcessor(p));
                }
            });

            while (!inferenceQueueProcessor.isEmpty()) {
                ProgressManager.checkCanceled();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }

        @Override
        public boolean shouldStartInBackground() {
            return true;
        }

        @Override
        public DumbModeAction getDumbModeAction() {
            return DumbModeAction.WAIT;
        }
    }
}
