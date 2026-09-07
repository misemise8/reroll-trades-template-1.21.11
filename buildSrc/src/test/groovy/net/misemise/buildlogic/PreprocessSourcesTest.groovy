package net.misemise.buildlogic

import org.junit.Test
import static org.junit.Assert.*

class PreprocessSourcesTest {
    @Test void selectsNestedBranchesAndKeepsLineNumbers() {
        def source = '''start
//#if MC >= 260100
modern
//#if MC >= 260200
latest
//#else
//$$ middle
//#endif
//#elseif MC >= 12109 && MC < 260100
//$$ keys
//#else
//$$ old
//#endif
end
'''
        [12100: ['start', 'old', 'end'], 12109: ['start', 'keys', 'end'],
         260100: ['start', 'modern', 'middle', 'end'], 260200: ['start', 'modern', 'latest', 'end']].each { mc, expected ->
            def result = PreprocessSources.expand(source, mc)
            assertEquals(expected, result.readLines().findAll { !it.empty })
            assertEquals(source.readLines().size(), result.readLines().size())
        }
    }
    @Test void rejectsBrokenDirectives() {
        ['//#if MC >= 12100\nx\n', '//#else\n', '//#if M == 1\n//#endif\n',
         '//#if MC > 0\n//#else\n//#elseif MC > 0\n//#endif\n'].each { source ->
            assertThrows(IllegalArgumentException) { PreprocessSources.expand(source, 260200) }
        }
    }
    @Test void handlesDisjunctionAndLeavesOrdinaryComments() {
        assertTrue(PreprocessSources.condition('MC < 12104 || MC >= 260100', 260200))
        assertFalse(PreprocessSources.condition('MC < 12104 || MC >= 260100', 12111))
        assertEquals('// ordinary\nint x = 1;\n', PreprocessSources.expand('// ordinary\nint x = 1;\n', 12100))
    }
}
