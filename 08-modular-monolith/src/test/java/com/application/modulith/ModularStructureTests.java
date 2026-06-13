package com.application.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularStructureTests {

    private final ApplicationModules modules = ApplicationModules.of(ModularMonolithApplication.class);

    @Test
    void shouldRespectModuleBoundaries() {
        // Fails the build if any module reaches into another module's internal packages.
        modules.verify();
    }

    @Test
    void generateModuleDocumentation() {
        // Produces PlantUML diagrams and AsciiDoc under target/spring-modulith-docs/
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml();
    }
}
