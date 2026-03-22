tasks.register("Delete", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
