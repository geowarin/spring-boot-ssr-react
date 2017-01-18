package geowarin.bootwebpack.extensions.path;

import java.io.IOException
import java.nio.file.Path
import java.util.*

/**
 * An enumeration to describe possible walk directions.
 * There are two of them: beginning from parents, ending with children,
 * and beginning from children, ending with parents. Both use depth-first search.
 */
public enum class FileWalkDirection {
    /** Depth-first search, directory is visited BEFORE its files */
    TOP_DOWN,
    /** Depth-first search, directory is visited AFTER its files */
    BOTTOM_UP
    // Do we want also breadth-first search?
}

/**
 * This class is intended to implement different path traversal methods.
 * It allows to iterate through all files inside a given directory.
 *
 * Use [Path.walk], [Path.walkTopDown] or [Path.walkBottomUp] extension functions to instantiate a `FileTreeWalk` instance.

 * If the file path given is just a file, walker iterates only it.
 * If the file path given does not exist, walker iterates nothing, i.e. it's equivalent to an empty sequence.
 */
public class FileTreeWalk private constructor(
        private val start: Path,
        private val direction: FileWalkDirection = FileWalkDirection.TOP_DOWN,
        private val onEnter: ((Path) -> Boolean)?,
        private val onLeave: ((Path) -> Unit)?,
        private val onFail: ((f: Path, e: IOException) -> Unit)?,
        private val maxDepth: Int = Int.MAX_VALUE
) : Sequence<Path> {

    internal constructor(start: Path, direction: FileWalkDirection = FileWalkDirection.TOP_DOWN) : this(start, direction, null, null, null)


    /** Returns an iterator walking through files. */
    override public fun iterator(): Iterator<Path> = FileTreeWalkIterator()

    /** Abstract class that encapsulates file visiting in some order, beginning from a given [root] */
    private abstract class WalkState(val root: Path) {
        /** Call of this function proceeds to a next file for visiting and returns it */
        abstract public fun step(): Path?
    }

    /** Abstract class that encapsulates directory visiting in some order, beginning from a given [rootDir] */
    private abstract class DirectoryState(rootDir: Path) : WalkState(rootDir) {
        init {
            assert(rootDir.isDirectory) { "rootDir must be verified to be directory beforehand." }
        }
    }

    private inner class FileTreeWalkIterator : AbstractIterator<Path>() {

        // Stack of directory states, beginning from the start directory
        private val state = Stack<WalkState>()

        init {
            if (start.isDirectory) {
                state.push(directoryState(start))
            } else if (start.isRegularFile) {
                state.push(SingleFileState(start))
            } else {
                done()
            }

        }

        override fun computeNext() {
            val nextFile = gotoNext()
            if (nextFile != null)
                setNext(nextFile)
            else
                done()
        }


        private fun directoryState(root: Path): DirectoryState {
            return when (direction) {
                FileWalkDirection.TOP_DOWN -> TopDownDirectoryState(root)
                FileWalkDirection.BOTTOM_UP -> BottomUpDirectoryState(root)
            }
        }

        tailrec private fun gotoNext(): Path? {

            if (state.empty()) {
                // There is nothing in the state
                return null
            }
            // Take next file from the top of the stack
            val topState = state.peek()!!
            val file = topState.step()
            if (file == null) {
                // There is nothing more on the top of the stack, go back
                state.pop()
                return gotoNext()
            } else {
                // Check that file/directory matches the filter
                if (file == topState.root || !file.isDirectory || state.size >= maxDepth) {
                    // Proceed to a root directory or a simple file
                    return file
                } else {
                    // Proceed to a sub-directory
                    state.push(directoryState(file))
                    return gotoNext()
                }
            }
        }

        /** Visiting in bottom-up order */
        private inner class BottomUpDirectoryState(rootDir: Path) : DirectoryState(rootDir) {

            private var rootVisited = false

            private var fileList: List<Path>? = null

            private var fileIndex = 0

            private var failed = false

            /** First all children, then root directory */
            override public fun step(): Path? {
                if (!failed && fileList == null) {
                    if (onEnter?.invoke(root) == false) {
                        return null
                    }

                    fileList = root.list()
                    if (fileList == null) {
                        onFail?.invoke(root, AccessDeniedException(file = root, reason = "Cannot list files in a directory"))
                        failed = true
                    }
                }
                if (fileList != null && fileIndex < fileList!!.size) {
                    // First visit all files
                    return fileList!![fileIndex++]
                } else if (!rootVisited) {
                    // Then visit root
                    rootVisited = true
                    return root
                } else {
                    // That's all
                    onLeave?.invoke(root)
                    return null
                }
            }
        }

        /** Visiting in top-down order */
        private inner class TopDownDirectoryState(rootDir: Path) : DirectoryState(rootDir) {

            private var rootVisited = false

            private var fileList: List<Path>? = null

            private var fileIndex = 0

            /** First root directory, then all children */
            override public fun step(): Path? {
                if (!rootVisited) {
                    // First visit root
                    if (onEnter?.invoke(root) == false) {
                        return null
                    }

                    rootVisited = true
                    return root
                } else if (fileList == null || fileIndex < fileList!!.size) {
                    if (fileList == null) {
                        // Then read an array of files, if any
                        fileList = root.list()
                        if (fileList == null) {
                            onFail?.invoke(root, AccessDeniedException(file = root, reason = "Cannot list files in a directory"))
                        }
                        if (fileList == null || fileList!!.isEmpty()) {
                            onLeave?.invoke(root)
                            return null
                        }
                    }
                    // Then visit all files
                    return fileList!![fileIndex++]
                } else {
                    // That's all
                    onLeave?.invoke(root)
                    return null
                }
            }
        }

        private inner class SingleFileState(rootFile: Path) : WalkState(rootFile) {
            private var visited: Boolean = false

            init {
                assert(rootFile.isRegularFile) { "rootFile must be verified to be file beforehand." }
            }

            override fun step(): Path? {
                if (visited) return null
                visited = true
                return root
            }
        }

    }

    /**
     * Sets a predicate [function], that is called on any entered directory before its files are visited
     * and before it is visited itself.
     *
     * If the [function] returns `false` the directory is not entered, and neither it nor its files are not visited.
     */
    public fun onEnter(function: (Path) -> Boolean): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter = function, onLeave = onLeave, onFail = onFail, maxDepth = maxDepth)
    }

    /**
     * Sets a callback [function], that is called on any left directory after its files are visited and after it is visited itself.
     */
    public fun onLeave(function: (Path) -> Unit): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter, function, onFail, maxDepth)
    }

    /**
     * Set a callback [function], that is called on a directory when it's impossible to get its file list.
     *
     * [onEnter] and [onLeave] callback functions are called even in this case.
     */
    public fun onFail(function: (Path, IOException) -> Unit): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter, onLeave, function, maxDepth)
    }

    /**
     * Sets the maximum [depth] of a directory tree to traverse. By default there is no limit.
     *
     * The value must be positive and [Int.MAX_VALUE] is used to specify an unlimited depth.
     *
     * With a value of 1, walker visits only the origin directory and all its immediate children,
     * with a value of 2 also grandchildren, etc.
     */
    public fun maxDepth(depth: Int): FileTreeWalk {
        if (depth <= 0)
            throw IllegalArgumentException("depth must be positive, but was $depth.")
        return FileTreeWalk(start, direction, onEnter, onLeave, onFail, depth)
    }
}

/**
 * Gets a sequence for visiting this directory and all its content.
 *
 * @param direction walk direction, top-down (by default) or bottom-up.
 */
public fun Path.walk(direction: FileWalkDirection = FileWalkDirection.TOP_DOWN): FileTreeWalk =
        FileTreeWalk(this, direction)

/**
 * Gets a sequence for visiting this directory and all its content in top-down order.
 * Depth-first search is used and directories are visited before all their files.
 */
public fun Path.walkTopDown(): FileTreeWalk = walk(FileWalkDirection.TOP_DOWN)

/**
 * Gets a sequence for visiting this directory and all its content in bottom-up order.
 * Depth-first search is used and directories are visited after all their files.
 */
public fun Path.walkBottomUp(): FileTreeWalk = walk(FileWalkDirection.BOTTOM_UP)
