import ru.testit.client.api.AutoTestsApi;
import ru.testit.client.api.WorkItemsApi;
import ru.testit.client.invoker.ApiException;
import ru.testit.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestItAutoTestCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestItAutoTestCreator.class);
    private final AutoTestsApi autoTestsApi;
    private final WorkItemsApi workItemsApi;
    // ... (остальные поля и конструктор без изменений)

    public AutoTestBuilder createAutoTest() {
        return new AutoTestBuilder(autoTestsApi, this);
    }

    /**
     * Внутренний класс-билдер для создания автотеста.
     */
    public static class AutoTestBuilder {
        private final AutoTestsApi autoTestsApi;
        private final TestItAutoTestCreator creator;
        private final AutoTestPostModel model = new AutoTestPostModel();
        private boolean createLinkedManualTestCase = false; // Новый флаг

        public AutoTestBuilder(AutoTestsApi autoTestsApi, TestItAutoTestCreator creator) {
            this.autoTestsApi = autoTestsApi;
            this.creator = creator;
        }

        /**
         * Указывает, нужно ли создать связанный ручной тест.
         * @param create Если true, после создания автотеста будет создан и связан ручной тест.
         * @return Этот билдер.
         */
        public AutoTestBuilder withLinkedManualTestCase(boolean create) {
            this.createLinkedManualTestCase = create;
            return this;
        }

        public AutoTestBuilder withExternalId(String externalId) {
            model.setExternalId(externalId);
            return this;
        }

        public AutoTestBuilder withProjectId(UUID projectId) {
            model.setProjectId(projectId);
            return this;
        }

        public AutoTestBuilder withName(String name) {
            model.setName(name);
            return this;
        }

        public AutoTestBuilder withNamespace(String namespace) {
            model.setNamespace(namespace);
            return this;
        }

        public AutoTestBuilder withClassname(String classname) {
            model.setClassname(classname);
            return this;
        }

        public AutoTestBuilder withSteps(List<AutoTestStepModel> steps) {
            model.setSteps(steps != null ? new ArrayList<>(steps) : null);
            return this;
        }

        public AutoTestBuilder addStep(String title, String description) {
            AutoTestStepModel step = new AutoTestStepModel();
            step.setTitle(title);
            step.setDescription(description);
            List<AutoTestStepModel> steps = model.getSteps();
            if (steps == null || steps.getClass().getName().contains("Unmodifiable")) {
                steps = new ArrayList<>();
                model.setSteps(steps);
            }
            steps.add(step);
            return this;
        }

        public AutoTestBuilder withSetup(List<AutoTestStepModel> setup) {
            model.setSetup(setup != null ? new ArrayList<>(setup) : null);
            return this;
        }

        public AutoTestBuilder addSetupStep(String title, String description) {
            AutoTestStepModel step = new AutoTestStepModel();
            step.setTitle(title);
            step.setDescription(description);
            List<AutoTestStepModel> setup = model.getSetup();
            if (setup == null || setup.getClass().getName().contains("Unmodifiable")) {
                setup = new ArrayList<>();
                model.setSetup(setup);
            }
            setup.add(step);
            return this;
        }

        public AutoTestBuilder withTeardown(List<AutoTestStepModel> teardown) {
            model.setTeardown(teardown != null ? new ArrayList<>(teardown) : null);
            return this;
        }

        public AutoTestBuilder addTeardownStep(String title, String description) {
            AutoTestStepModel step = new AutoTestStepModel();
            step.setTitle(title);
            step.setDescription(description);
            List<AutoTestStepModel> teardown = model.getTeardown();
            if (teardown == null || teardown.getClass().getName().contains("Unmodifiable")) {
                teardown = new ArrayList<>();
                model.setTeardown(teardown);
            }
            teardown.add(step);
            return this;
        }

        public AutoTestBuilder withTitle(String title) {
            model.setTitle(title);
            return this;
        }

        public AutoTestBuilder withDescription(String description) {
            model.setDescription(description);
            return this;
        }

        public AutoTestBuilder withLabels(List<LabelPostModel> labels) {
            model.setLabels(labels != null ? new ArrayList<>(labels) : null);
            return this;
        }

        public AutoTestBuilder addLabel(String name) {
            LabelPostModel label = new LabelPostModel();
            label.setName(name);
            if (model.getLabels() == null) {
                model.setLabels(new ArrayList<>());
            }
            model.getLabels().add(label);
            return this;
        }

        public AutoTestBuilder withIsFlaky(boolean isFlaky) {
            model.setIsFlaky(isFlaky);
            return this;
        }

        public AutoTestBuilder withExternalKey(String externalKey) {
            model.setExternalKey(externalKey);
            return this;
        }

        public AutoTestBuilder linkToJiraTask(String jiraTaskKey) {
            if (model.getProjectId() == null) {
                LOGGER.error("Не указан projectId для привязки к Jira задаче {}", jiraTaskKey);
                return this;
            }
            Optional<UUID> workItemId = creator.getWorkItemIdByExternalId(model.getProjectId(), jiraTaskKey);
            if (workItemId.isPresent()) {
                if (model.getWorkItemIds() == null) {
                    model.setWorkItemIds(new ArrayList<>());
                }
                model.getWorkItemIds().add(workItemId.get().toString());
                LOGGER.info("Привязка автотеста к Jira задаче {} (workItemId: {})", jiraTaskKey, workItemId.get());
            } else {
                LOGGER.warn("Не удалось привязать автотест к Jira задаче {}", jiraTaskKey);
            }
            return this;
        }

        public AutoTestBuilder addLink(String url, String title, String type) {
            if (url == null || title == null || type == null) {
                LOGGER.error("URL, title или type ссылки не могут быть null");
                return this;
            }
            LinkPostModel link = new LinkPostModel();
            link.setUrl(url);
            link.setTitle(title);
            link.setType(type);
            if (model.getLinks() == null) {
                model.setLinks(new ArrayList<>());
            }
            model.getLinks().add(link);
            LOGGER.info("Добавлена ссылка к автотесту: {} ({})", title, url);
            return this;
        }

        /**
         * Создает или обновляет автотест в TestIt TMS. Если createLinkedManualTestCase=true,
         * создает и связывает ручной тест.
         * @return Optional с моделью созданного или обновленного автотеста или пустой, если произошла ошибка.
         */
        public Optional<AutoTestModel> build() {
            if (model.getExternalId() == null || model.getProjectId() == null || model.getName() == null) {
                LOGGER.error("Отсутствуют обязательные поля: externalId, projectId, name");
                return Optional.empty();
            }

            try {
                AutoTestFilterApiModel filter = new AutoTestFilterApiModel();
                filter.setExternalIds(Set.of(model.getExternalId()));
                filter.setProjectIds(Set.of(model.getProjectId().toString()));
                AutoTestSearchIncludeApiModel includes = new AutoTestSearchIncludeApiModel()
                        .includeLabels(true)
                        .includeSteps(true)
                        .includeLinks(true);
                AutoTestSearchApiModel body = new AutoTestSearchApiModel()
                        .filter(filter)
                        .includes(includes);
                List<AutoTestApiResult> existingTests = autoTestsApi.apiV2AutoTestsSearchPost(
                        null, null, null, null, null, body
                );

                AutoTestModel result;
                if (!existingTests.isEmpty()) {
                    AutoTestApiResult existing = existingTests.get(0);
                    AutoTestPutModel putModel = new AutoTestPutModel();
                    putModel.setId(existing.getId());
                    putModel.setExternalId(model.getExternalId());
                    putModel.setProjectId(model.getProjectId());
                    putModel.setName(model.getName());
                    putModel.setNamespace(model.getNamespace());
                    putModel.setClassname(model.getClassname());
                    putModel.setSteps(model.getSteps() != null ? new ArrayList<>(model.getSteps()) : null);
                    putModel.setSetup(model.getSetup() != null ? new ArrayList<>(model.getSetup()) : null);
                    putModel.setTeardown(model.getTeardown() != null ? new ArrayList<>(model.getTeardown()) : null);
                    putModel.setTitle(model.getTitle());
                    putModel.setDescription(model.getDescription());
                    putModel.setLabels(model.getLabels() != null ? new ArrayList<>(model.getLabels()) : null);
                    putModel.setIsFlaky(model.getIsFlaky());
                    putModel.setExternalKey(model.getExternalKey());
                    List<LinkPutModel> putLinks = model.getLinks() != null ? model.getLinks().stream()
                            .map(link -> {
                                LinkPutModel putLink = new LinkPutModel();
                                putLink.setUrl(link.getUrl());
                                putLink.setTitle(link.getTitle());
                                putLink.setType(link.getType());
                                return putLink;
                            })
                            .collect(Collectors.toList()) : null;
                    putModel.setLinks(putLinks);

                    LOGGER.info("Обновление существующего автотеста с ID: {}", existing.getId());
                    autoTestsApi.updateAutoTest(putModel);
                    result = existing;
                } else {
                    LOGGER.info("Создание нового автотеста с externalId: {}", model.getExternalId());
                    result = autoTestsApi.createAutoTest(model);
                    LOGGER.info("Автотест создан с ID: {}", result.getId());

                    // Создание связанного ручного теста, если флаг включен
                    if (createLinkedManualTestCase) {
                        try {
                            WorkItemPostModel testCase = new WorkItemPostModel();
                            testCase.setProjectId(model.getProjectId());
                            testCase.setName(model.getName() + " (Manual)");
                            testCase.setEntityTypeName("TestCase");
                            // Копирование шагов из автотеста, если они есть
                            if (model.getSteps() != null && !model.getSteps().isEmpty()) {
                                List<WorkItemStepModel> manualSteps = model.getSteps().stream()
                                        .map(step -> {
                                            WorkItemStepModel manualStep = new WorkItemStepModel();
                                            manualStep.setName(step.getTitle());
                                            manualStep.setDescription(step.getDescription());
                                            return manualStep;
                                        })
                                        .collect(Collectors.toList());
                                testCase.setSteps(manualSteps);
                            }

                            WorkItemModel createdTestCase = creator.workItemsApi.createWorkItem(testCase);
                            LOGGER.info("Создан ручной тест с ID: {}", createdTestCase.getId());

                            // Связывание ручного теста с автотестом
                            creator.autoTestsApi.apiV2AutoTestsIdWorkItemsPost(
                                    result.getId().toString(),
                                    createdTestCase.getId().toString()
                            );
                            LOGGER.info("Ручной тест с ID: {} связан с автотестом с ID: {}", 
                                        createdTestCase.getId(), result.getId());
                        } catch (ApiException e) {
                            LOGGER.error("Ошибка при создании или связывании ручного теста: {}", e.getMessage(), e);
                        }
                    }
                }
                return Optional.of(result);
            } catch (ApiException e) {
                LOGGER.error("Ошибка при создании/обновлении автотеста: {}", e.getMessage(), e);
                return Optional.empty();
            }
        }
    }

    // ... (остальные методы и класс StepBuilder без изменений)
}
