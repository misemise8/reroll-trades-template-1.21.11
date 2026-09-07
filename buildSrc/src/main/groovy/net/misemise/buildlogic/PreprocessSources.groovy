package net.misemise.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/** Expands MC-only Java comment directives without rewriting editable sources. */
@CacheableTask
abstract class PreprocessSources extends DefaultTask {
    @InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSourceRoots()
    @Input abstract Property<Integer> getMinecraftVersion()
    @OutputDirectory abstract DirectoryProperty getOutputDirectory()

    static boolean condition(String expression, int mc) {
        expression.split(/\s*\|\|\s*/, -1).any { disjunction ->
            disjunction.split(/\s*&&\s*/, -1).every { comparison ->
                def match = comparison.trim() =~ /^MC\s*(>=|<=|==|!=|>|<)\s*(\d+)$/
                if (!match.matches()) throw new IllegalArgumentException("Invalid MC condition: ${expression}")
                int value = Integer.parseInt(match.group(2))
                switch (match.group(1)) {
                    case '>=': return mc >= value
                    case '<=': return mc <= value
                    case '==': return mc == value
                    case '!=': return mc != value
                    case '>': return mc > value
                    case '<': return mc < value
                    default: throw new IllegalArgumentException(expression)
                }
            }
        }
    }

    static String expand(String source, int mc) {
        def stack = []
        boolean active = true
        def output = new StringBuilder()
        source.readLines().eachWithIndex { line, index ->
            def directive = line.trim() =~ /^\/\/#(if|elseif|else|endif)(?:\s+(.*))?$/
            if (directive.matches()) {
                String kind = directive.group(1)
                String expr = directive.group(2)
                if (kind == 'if') {
                    boolean selected = condition(expr ?: '', mc)
                    stack << [parent: active, taken: selected, hasElse: false]
                    active = active && selected
                } else {
                    if (stack.empty) throw new IllegalArgumentException("Unexpected ${kind} at line ${index + 1}")
                    def frame = stack.last()
                    if (kind == 'endif') {
                        if (expr) throw new IllegalArgumentException("Unexpected endif expression at line ${index + 1}")
                        active = frame.parent
                        stack.remove(stack.size() - 1)
                    } else {
                        if (frame.hasElse) throw new IllegalArgumentException("Branch after else at line ${index + 1}")
                        boolean selected
                        if (kind == 'else') {
                            if (expr) throw new IllegalArgumentException("Unexpected else expression at line ${index + 1}")
                            selected = true
                            frame.hasElse = true
                        } else {
                            selected = condition(expr ?: '', mc)
                        }
                        active = frame.parent && !frame.taken && selected
                        frame.taken = frame.taken || selected
                    }
                }
                output.append('\n') // Preserve original source line numbers in diagnostics.
            } else if (active) {
                output.append(line.replaceFirst(/^(\s*)\/\/\$\$ ?/, '$1')).append('\n')
            } else {
                output.append('\n')
            }
        }
        if (!stack.empty) throw new IllegalArgumentException('Missing //#endif')
        return output.toString()
    }

    @TaskAction
    void generate() {
        def destination = outputDirectory.get().asFile
        project.delete(destination) // This task's generated directory only.
        def seen = new HashSet<String>()
        sourceRoots.files.each { root ->
            project.fileTree(root).matching { include '**/*.java' }.files.sort().each { source ->
                String relative = root.toPath().relativize(source.toPath()).toString()
                if (!seen.add(relative)) throw new GradleException("Duplicate source: ${relative}")
                def target = new File(destination, relative)
                target.parentFile.mkdirs()
                try {
                    target.setText(expand(source.getText('UTF-8'), minecraftVersion.get()), 'UTF-8')
                } catch (IllegalArgumentException e) {
                    throw new GradleException("${source}: ${e.message}", e)
                }
            }
        }
    }
}
