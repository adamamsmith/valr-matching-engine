package smith.adam.model.orderbook.tree

import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayDeque

class RedBlackTree<T : Comparable<T>> {
    enum class Colour {
        RED, BLACK
    }

    data class Node<T : Comparable<T>>(var data: T, var colour: Colour) {
        var left: Node<T>? = null
        var right: Node<T>? = null
        var parent: Node<T>? = null

        fun isOnLeft(): Boolean {
            return this.parent?.left == this
        }

        fun isOnRight(): Boolean {
            return this.parent?.right == this
        }
    }

    private var root: Node<T>? = null

    fun add(data: T): Node<T> {
        val newNode = Node(data, Colour.RED)
        root = insertNode(root, newNode)
        fixInsert(newNode)
        return newNode
    }

    fun delete(node: Node<T>): Node<T> {
        val deletedNode = deleteNode(node)
        fixDelete(deletedNode)

        return node
    }

    fun iteratorFromNode(node: Node<T>?, reverse: Boolean = false): Iterator<Node<T>> {
        if (node == null) return Collections.emptyIterator()

        return object : Iterator<Node<T>> {
            private val stack = ArrayDeque<Node<T>>()
            private var currentTraversalRoot: Node<T>? = node

            init {
                stack.addLast(node)
            }

            override fun hasNext(): Boolean {
                return stack.isNotEmpty()
            }

            override fun next(): Node<T> {
                if (!hasNext()) throw NoSuchElementException()
                val nextNode = stack.removeLast()

                if (reverse) {
                    if (nextNode.left != null) {
                        pushRightNodes(nextNode.left)
                    }
                } else {
                    if (nextNode.right != null) {
                        pushLeftNodes(nextNode.right)
                    }
                }

                if (stack.isEmpty()) {
                    updateCurrentTraversalRoot()
                }

                return nextNode
            }

            private fun pushLeftNodes(node: Node<T>?) {
                var current = node
                while (current != null) {
                    stack.addLast(current)
                    current = current.left
                }
            }

            private fun pushRightNodes(node: Node<T>?) {
                var current = node
                while (current != null) {
                    // Editor complains if I don't put a non-null assertions here even though it shouldn't be needed.
                    stack.addLast(current!!)
                    current = current!!.right
                }
            }

            private fun updateCurrentTraversalRoot(reverse: Boolean = false) {
                if (reverse) {
                    while (currentTraversalRoot != null && currentTraversalRoot!!.isOnLeft()) {
                        currentTraversalRoot = currentTraversalRoot!!.parent
                    }
                } else {
                    while (currentTraversalRoot != null && currentTraversalRoot!!.isOnRight()) {
                        currentTraversalRoot = currentTraversalRoot!!.parent
                    }
                }

                currentTraversalRoot = currentTraversalRoot!!.parent
                if (currentTraversalRoot != null) {
                    stack.addLast(currentTraversalRoot!!)
                }
            }
        }
    }

    fun <U> toList(mutator: (T) -> List<U>): List<U> {
        return traverse(root, mutator = mutator)
    }

    fun visualizeTree() {
        if (root == null) {
            println("The tree is empty.")
            return
        }
        printTree(root, "", true)
    }

    private fun printTree(node: Node<T>?, indent: String, isRight: Boolean) {
        if (node == null) return

        println(indent + (if (isRight) "└── " else "├── ") + "${node.data} (${node.colour})")

        val childIndent = indent + if (isRight) "    " else "│   "
        printTree(node.right, childIndent, false)
        printTree(node.left, childIndent, true)
    }

    private fun insertNode(root: Node<T>?, newNode: Node<T>): Node<T> {
        if (root == null) return newNode

        if (newNode.data < root.data) {
            root.left = insertNode(root.left, newNode)
            root.left!!.parent = root
        } else if (newNode.data > root.data) {
            root.right = insertNode(root.right, newNode)
            root.right!!.parent = root
        }
        return root
    }

    private fun fixInsert(node: Node<T>) {
        var currentNode = node

        while (currentNode.parent?.colour == Colour.RED) {
            val parent = currentNode.parent!!
            val grandparent = parent.parent

            if (parent.isOnLeft()) {
                val uncle = grandparent!!.right

                if (uncle?.colour == Colour.RED) {
                    parent.colour = Colour.BLACK
                    uncle.colour = Colour.BLACK
                    grandparent.colour = Colour.RED
                    currentNode = grandparent
                } else {
                    if (currentNode == parent.right) {
                        rotateLeft(parent)
                        currentNode = parent
                    }
                    rotateRight(grandparent)
                    parent.colour = Colour.BLACK
                    grandparent.colour = Colour.RED
                }
            } else if (parent.isOnRight()) {
                val uncle = grandparent!!.left

                if (uncle?.colour == Colour.RED) {
                    parent.colour = Colour.BLACK
                    uncle.colour = Colour.BLACK
                    grandparent.colour = Colour.RED
                    currentNode = grandparent
                } else {
                    if (currentNode == parent.left) {
                        rotateRight(parent)
                        currentNode = parent
                    }
                    rotateLeft(grandparent)
                    parent.colour = Colour.BLACK
                    grandparent.colour = Colour.RED
                }
            }
        }

        root?.colour = Colour.BLACK
    }

    private fun deleteNode(node: Node<T>): Node<T> {
        val deletedNode: Node<T>?

        if (node.left == null || node.right == null) {
            deletedNode = node
        } else {
            deletedNode = minimum(node.right!!)
            node.data = deletedNode.data
        }

        val child: Node<T>? = if (deletedNode.left != null) {
            deletedNode.left
        } else {
            deletedNode.right
        }

        if (child != null) {
            child.parent = deletedNode.parent
        }

        if (deletedNode.parent == null) {
            root = child
        } else if (deletedNode.isOnLeft()) {
            deletedNode.parent!!.left = child
        } else {
            deletedNode.parent!!.right = child
        }

        return deletedNode
    }

    private fun fixDelete(node: Node<T>) {
        var currentNode = node
        while (currentNode != root && currentNode.colour == Colour.BLACK) {
            if (currentNode.isOnLeft()) {
                var sibling = currentNode.parent!!.right
                if (sibling!!.colour == Colour.RED) {
                    sibling.colour = Colour.BLACK
                    currentNode.parent!!.colour = Colour.RED
                    rotateLeft(currentNode.parent)
                    sibling = currentNode.parent!!.right
                }
                if ((sibling!!.left == null || sibling.left!!.colour == Colour.BLACK) &&
                    (sibling.right == null || sibling.right!!.colour == Colour.BLACK)
                ) {
                    sibling.colour = Colour.RED
                    currentNode = currentNode.parent!!
                } else {
                    if (sibling.right == null || sibling.right!!.colour == Colour.BLACK) {
                        sibling.left!!.colour = Colour.BLACK
                        sibling.colour = Colour.RED
                        rotateRight(sibling)
                        sibling = currentNode.parent!!.right
                    }
                    sibling!!.colour = currentNode.parent!!.colour
                    currentNode.parent!!.colour = Colour.BLACK
                    sibling.right!!.colour = Colour.BLACK
                    rotateLeft(currentNode.parent)
                    currentNode = root!!
                }
            } else {
                var sibling = currentNode.parent!!.left
                if (sibling!!.colour == Colour.RED) {
                    sibling.colour = Colour.BLACK
                    currentNode.parent!!.colour = Colour.RED
                    rotateRight(currentNode.parent)
                    sibling = currentNode.parent!!.left
                }
                if ((sibling!!.right == null || sibling.right!!.colour == Colour.BLACK) &&
                    (sibling.left == null || sibling.left!!.colour == Colour.BLACK)
                ) {
                    sibling.colour = Colour.RED
                    currentNode = currentNode.parent!!
                } else {
                    if (sibling.left == null || sibling.left!!.colour == Colour.BLACK) {
                        sibling.right!!.colour = Colour.BLACK
                        sibling.colour = Colour.RED
                        rotateLeft(sibling)
                        sibling = currentNode.parent!!.left
                    }
                    sibling!!.colour = currentNode.parent!!.colour
                    currentNode.parent!!.colour = Colour.BLACK
                    sibling.left!!.colour = Colour.BLACK
                    rotateRight(currentNode.parent)
                    currentNode = root!!
                }
            }
        }
        currentNode.colour = Colour.BLACK
    }

    private fun minimum(node: Node<T>): Node<T> {
        var temp = node
        while (temp.left != null) {
            temp = temp.left!!
        }
        return temp
    }

    private fun rotateLeft(node: Node<T>?) {
        val rightChild = node?.right ?: return

        node.right = rightChild.left
        if (rightChild.left != null) rightChild.left!!.parent = node

        rightChild.parent = node.parent

        if (node.parent == null) {
            root = rightChild
        } else if (node == node.parent!!.left) {
            node.parent!!.left = rightChild
        } else {
            node.parent!!.right = rightChild
        }

        rightChild.left = node
        node.parent = rightChild
    }

    private fun rotateRight(node: Node<T>?) {
        val leftChild = node?.left ?: return

        node.left = leftChild.right
        if (leftChild.right != null) leftChild.right!!.parent = node

        leftChild.parent = node.parent

        if (node.parent == null) {
            root = leftChild
        } else if (node == node.parent!!.right) {
            node.parent!!.right = leftChild
        } else {
            node.parent!!.left = leftChild
        }

        leftChild.right = node
        node.parent = leftChild
    }

    private fun <U> traverse(node: Node<T>?, mutator: (T) -> List<U>): List<U> {
        if (node == null) return emptyList()

        val result = mutableListOf<U>()

        result.addAll(traverse(node.left, mutator = mutator))
        result.addAll(mutator(node.data))
        result.addAll(traverse(node.right, mutator = mutator))

        return result
    }
}
