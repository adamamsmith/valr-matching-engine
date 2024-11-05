package orderbook.tree

import org.junit.jupiter.api.Test

import smith.adam.model.orderbook.tree.RedBlackTree

class RedBlackTreeTest {

    fun <T: Comparable<T>> iteratorPrinter(it: Iterator<RedBlackTree.Node<T>>) {
        while (it.hasNext()) {
            print("" + it.next().data + " ")
        }
    }

    @Test
    fun visualizeTree() {
        val tree = RedBlackTree<Int>()

        val node1 = tree.add(1)
        tree.visualizeTree()
        println()
        tree.add(2)
        val node3 = tree.add(3)
        tree.visualizeTree()
        println()
        println(tree.toList( { listOf(it) }))

        val node10 = tree.add(10)
        tree.visualizeTree()
        println()
        tree.add(20)
        tree.visualizeTree()
        println()
        println(tree.toList( { listOf(it) }))

        val node30 = tree.add(30)
        var node40 = tree.add(40)
        tree.visualizeTree()
        println()
        tree.add(25)
        tree.visualizeTree()
        println()

        node40 = tree.delete(node30)
        tree.visualizeTree()
        println()

        println(tree.toList ({ listOf(it) }))

        val it1 = tree.iteratorFromNode(node1)
        iteratorPrinter(it1)
        println()

        var it3 = tree.iteratorFromNode(node3)
        iteratorPrinter(it3)
        println()

        val it40 = tree.iteratorFromNode(node40)
        iteratorPrinter(it40)
        println()

        val it10 = tree.iteratorFromNode(node10)
        iteratorPrinter(it10)
        println()

        val it10rev = tree.iteratorFromNode(node10, reverse = true)
        iteratorPrinter(it10rev)
        println()

        val node = tree.delete(node10)
        println(node.data)
        println()

        it3 = tree.iteratorFromNode(node3)
        iteratorPrinter(it3)
        println()
    }
}