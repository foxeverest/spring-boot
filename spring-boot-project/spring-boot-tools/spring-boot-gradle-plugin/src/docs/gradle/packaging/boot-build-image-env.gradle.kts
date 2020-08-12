import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

// tag::env[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	environment = mapOf("BP_JVM_VERSION" to "13.0.1")
}
// end::env[]

tasks.register("bootBuildImageEnvironment") {
	doFirst {
		for((name, value) in tasks.getByName<BootBuildImage>("bootBuildImage").environment) {
			print(name + "=" + value)
		}
	}
}

