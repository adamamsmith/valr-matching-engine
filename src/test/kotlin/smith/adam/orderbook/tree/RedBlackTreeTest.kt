package smith.adam.orderbook.tree

import org.junit.jupiter.api.BeforeEach
import smith.adam.orderbook.tree.RedBlackTree.Node
import kotlin.test.*

class RedBlackTreeTest {

    private lateinit var tree: RedBlackTree<Int>

    @BeforeEach
    fun setUp() {
        tree = RedBlackTree()
    }

    @Test
    fun `add - simple`() {
        tree.add(10)
        val values = tree.toList { listOf(it) }
        assertTrue(10 in values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add repeat`() {
        tree.add(10)
        tree.add(10)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add left left`() {
        tree.add(30)
        tree.add(20)
        tree.add(10)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 20, 30), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add left left-right`() {
        tree.add(30)
        tree.add(10)
        tree.add(20)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 20, 30), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add right right`() {
        tree.add(10)
        tree.add(20)
        tree.add(30)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 20, 30), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add right right-left`() {
        tree.add(10)
        tree.add(30)
        tree.add(20)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 20, 30), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add left right`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 20, 30), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add left right left-left`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(5)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(5, 10, 20, 30), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add left right left-right`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(15)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 15, 20, 30), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add left right right-left`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(25)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 20, 25, 30), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `add - add left right right-right`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(35)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 20, 30, 35), values)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `delete - delete root node`() {
        val node = tree.add(10)
        tree.delete(node)
        val values = tree.toList { listOf(it) }
        assertTrue(values.isEmpty())
        assertRedBlackProperties(tree)
    }

    @Test
    fun `delete - delete leaf node`() {
        tree.add(20)
        val node = tree.add(10)
        tree.add(30)
        tree.delete(node)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(20, 30), values)
        assertTrue(tree.root?.data == 20)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `delete - delete left node left child`() {
        tree.add(20)
        val node = tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.delete(node)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(5, 20, 30), values)
        assertTrue(tree.root?.data == 20)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `delete - delete left node right child`() {
        tree.add(20)
        val node = tree.add(10)
        tree.add(30)
        tree.add(15)
        tree.delete(node)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(15, 20, 30), values)
        assertTrue(tree.root?.data == 20)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `delete - delete right node left child`() {
        tree.add(20)
        tree.add(10)
        val node = tree.add(30)
        tree.add(25)
        tree.delete(node)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 20, 25), values)
        assertTrue(tree.root?.data == 20)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `delete - delete right node right child`() {
        tree.add(20)
        tree.add(10)
        val node = tree.add(30)
        tree.add(35)
        tree.delete(node)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 20, 35), values)
        assertTrue(tree.root?.data == 20)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `delete - delete root two children`() {
        val node = tree.add(20)
        tree.add(30)
        tree.add(10)
        tree.delete(node)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(10, 30), values)
        assertTrue(tree.root?.data == 30)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `delete - delete left node two children`() {
        tree.add(20)
        val node = tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)
        tree.delete(node)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(5, 15, 20, 25, 30, 35), values)
        assertTrue(tree.root?.data == 20)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `delete - delete right node two children`() {
        tree.add(20)
        tree.add(10)
        val node = tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)
        tree.delete(node)
        val values = tree.toList { listOf(it) }
        assertEquals(listOf(5, 10, 15, 20, 25, 35), values)
        assertTrue(tree.root?.data == 20)
        assertRedBlackProperties(tree)
    }

    @Test
    fun `iteratorFromNode - leftmost leaf`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        val node = tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(5, 10, 15, 20, 25, 30, 35)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - rightmost leaf`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        val node = tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(35)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - left leaf`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(5)
        val node = tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(15, 20, 25, 30, 35)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - right leaf`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.add(15)
        val node = tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(25, 30, 35)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - right node`() {
        tree.add(20)
        val node = tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(10, 15, 20, 25, 30, 35)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - left node`() {
        tree.add(20)
        tree.add(10)
        val node = tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(30, 35)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - root`() {
        val node = tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(20, 25, 30, 35)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - leftmost leaf reverse`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        val node = tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node, reverse = true)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(5)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - rightmost leaf reverse`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        val node = tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node, reverse = true)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(35, 30, 25, 20, 15, 10, 5)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - left leaf reverse`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(5)
        val node = tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node, reverse = true)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(15, 10, 5)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - right leaf reverse`() {
        tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.add(15)
        val node = tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node, reverse = true)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(25, 20, 15, 10, 5)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - right node reverse`() {
        tree.add(20)
        val node = tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node, reverse = true)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(10, 5)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - left node reverse`() {
        tree.add(20)
        tree.add(10)
        val node = tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node, reverse = true)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(30, 25, 20, 15, 10, 5)
        assertEquals(expected, listFromNode)
    }

    @Test
    fun `iteratorFromNode - root reverse`() {
        val node = tree.add(20)
        tree.add(10)
        tree.add(30)
        tree.add(5)
        tree.add(15)
        tree.add(25)
        tree.add(35)

        val listFromNode = mutableListOf<Int>()
        val it = tree.iteratorFromNode(node, reverse = true)
        while (it.hasNext()) {
            listFromNode.add(it.next().data)
        }
        val expected = listOf(20, 15, 10, 5)
        assertEquals(expected, listFromNode)
    }

    // Helper Functions for testing
    private fun <T : Comparable<T>> assertRedBlackProperties(tree: RedBlackTree<T>) {
        assertRootIsBlack(tree)
        assertBlackHeightConsistent(tree)
        assertNoRedRedParentChild(tree)
    }

    private fun <T : Comparable<T>> assertRootIsBlack(tree: RedBlackTree<T>) {
        if (tree.root != null) {
            assertEquals(RedBlackTree.Colour.BLACK, tree.root?.colour, "Root node must be black")
        }
    }

    private fun <T : Comparable<T>> assertBlackHeightConsistent(tree: RedBlackTree<T>) {
        computeBlackHeight(tree.root)
    }

    private fun <T : Comparable<T>> computeBlackHeight(node: RedBlackTree.Node<T>?): Int {
        if (node == null) return 1
        val leftHeight = computeBlackHeight(node.left)
        val rightHeight = computeBlackHeight(node.right)
        if (leftHeight != rightHeight) {
            fail("Black height is inconsistent")
        }
        return leftHeight + if (node.colour == RedBlackTree.Colour.BLACK) 1 else 0
    }

    private fun <T : Comparable<T>> assertNoRedRedParentChild(tree: RedBlackTree<T>) {
        checkNoRedRedParentChild(tree.root)
    }

    private fun <T : Comparable<T>> checkNoRedRedParentChild(node: RedBlackTree.Node<T>?) {
        if (node == null) return
        if (node.colour == RedBlackTree.Colour.RED) {
            assertNotEquals(RedBlackTree.Colour.RED, node.left?.colour, "RED node with RED left child")
            assertNotEquals(RedBlackTree.Colour.RED, node.right?.colour, "RED node with RED right child")
        }
        checkNoRedRedParentChild(node.left)
        checkNoRedRedParentChild(node.right)
    }

    // Helper functions for tree visualization
    private fun <T : Comparable<T>> RedBlackTree<T>.visualize() {
        if (this.root == null) {
            println("The tree is empty.")
            return
        }
        printTree(root, "", true)
    }

    private fun <T : Comparable<T>> RedBlackTree<T>.printTree(node: Node<T>?, indent: String, isRight: Boolean) {
        if (node == null) return

        println(indent + (if (isRight) "└── " else "├── ") + "${node.data} (${node.colour})")

        val childIndent = indent + if (isRight) "    " else "│   "
        printTree(node.right, childIndent, false)
        printTree(node.left, childIndent, true)
    }
}
