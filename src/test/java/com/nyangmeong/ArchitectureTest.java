package com.nyangmeong;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AGENTS.md 3장(의존 방향)과 docs/convention/service.md의 @Transactional 규칙을 검사한다.
 * 실패하면 이 테스트가 아니라 메시지에 나온 코드를 고친다.
 */
class ArchitectureTest {

    private static final String ROOT = "com.nyangmeong";

    // AGENTS.md 3.1절의 층 표. 도메인을 새로 만들면 표와 여기를 같이 고친다
    private static final Map<String, Integer> DOMAIN_LAYERS = domainLayers();

    // 트랜잭션이 필요 없는 Service public 메서드. "클래스이름.메서드이름"으로 적는다
    private static final Set<String> TRANSACTIONAL_EXCEPTIONS = Set.of();

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    private static Map<String, Integer> domainLayers() {
        Map<String, Integer> layers = new LinkedHashMap<>();
        layers.put("member", 0);
        layers.put("pet", 1);
        layers.put("health", 2);
        layers.put("place", 2);
        layers.put("notification", 2);
        layers.put("expense", 3);
        layers.put("community", 3);
        layers.put("support", 3);
        layers.put("ai", 4);
        layers.put("admin", 5);
        layers.put("job", 5);
        return layers;
    }

    private static String domainPackage(String domain) {
        return ROOT + "." + domain + "..";
    }

    // 모든 도메인에서 이름이 layer인 패키지. layer가 비어 있으면 도메인 전체
    private static String[] domainPackages(String layer) {
        String suffix = layer.isEmpty() ? ".." : ".." + layer + "..";
        return DOMAIN_LAYERS.keySet().stream()
                .map(domain -> ROOT + "." + domain + suffix)
                .toArray(String[]::new);
    }

    @Test
    void global은_도메인을_참조하지_않는다() {
        noClasses().that().resideInAPackage(ROOT + ".global..")
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages(""))
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void 도메인_안에서는_controller_service_mapper_순서로만_부른다() {
        noClasses().that().resideInAnyPackage(domainPackages("controller"))
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages("mapper"))
                .allowEmptyShould(true)
                .check(CLASSES);
        noClasses().that().resideInAnyPackage(domainPackages("service"))
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages("controller"))
                .allowEmptyShould(true)
                .check(CLASSES);
        noClasses().that().resideInAnyPackage(domainPackages("mapper"))
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages("controller"))
                .allowEmptyShould(true)
                .check(CLASSES);
        noClasses().that().resideInAnyPackage(domainPackages("mapper"))
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages("service"))
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void 다른_도메인의_mapper를_참조하지_않는다() {
        for (String domain : DOMAIN_LAYERS.keySet()) {
            String[] otherMappers = DOMAIN_LAYERS.keySet().stream()
                    .filter(other -> !other.equals(domain))
                    .map(other -> ROOT + "." + other + "..mapper..")
                    .toArray(String[]::new);

            noClasses().that().resideInAPackage(domainPackage(domain))
                    .should().dependOnClassesThat().resideInAnyPackage(otherMappers)
                    .allowEmptyShould(true)
                    .check(CLASSES);
        }
    }

    @Test
    void 도메인은_자기보다_낮은_층만_참조한다() {
        for (Map.Entry<String, Integer> entry : DOMAIN_LAYERS.entrySet()) {
            String domain = entry.getKey();
            int layer = entry.getValue();
            // 같은 층끼리도 부르지 않는다
            String[] sameOrHigher = DOMAIN_LAYERS.entrySet().stream()
                    .filter(other -> !other.getKey().equals(domain) && other.getValue() >= layer)
                    .map(other -> domainPackage(other.getKey()))
                    .toArray(String[]::new);
            if (sameOrHigher.length == 0) {
                continue;
            }

            noClasses().that().resideInAPackage(domainPackage(domain))
                    .should().dependOnClassesThat().resideInAnyPackage(sameOrHigher)
                    .allowEmptyShould(true)
                    .check(CLASSES);
        }
    }

    @Test
    void Service의_public_메서드에는_Transactional이_있다() {
        DescribedPredicate<JavaMethod> listedAsException = new DescribedPredicate<>("예외 목록에 있는 메서드") {
            @Override
            public boolean test(JavaMethod method) {
                return TRANSACTIONAL_EXCEPTIONS.contains(
                        method.getOwner().getSimpleName() + "." + method.getName());
            }
        };

        methods().that().arePublic()
                .and().areDeclaredInClassesThat().resideInAnyPackage(domainPackages(""))
                .and().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                .and(DescribedPredicate.not(listedAsException))
                .should().beAnnotatedWith(Transactional.class)
                .allowEmptyShould(true)
                .check(CLASSES);
    }
}
