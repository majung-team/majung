package app.majung.server.pickup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ClientConfigurationApi {
    record Configuration(boolean developmentToolsEnabled) { }
    private final DeploymentPolicy deploymentPolicy;

    ClientConfigurationApi(DeploymentPolicy deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    @GetMapping(value = "/api/v1/config", produces = "application/json")
    Configuration configuration() {
        return new Configuration(deploymentPolicy.developmentToolsEnabled());
    }
}
