import graphviz


class Node:
    _id_counter = 0

    def __init__(self, is_leaf=True):
        self.is_leaf = is_leaf
        self.keys = []  # For leaves: actual data keys; for internals: "separator" keys
        self.children = []  # For internals: child pointers
        self.id = Node._get_next_id()

    @classmethod
    def _get_next_id(cls):
        cls._id_counter += 1
        return cls._id_counter

    def __str__(self):
        if self.is_leaf:
            return f"Leaf({self.keys})"
        else:
            return f"Internal({self.keys})"


class BPlusTree:
    def __init__(self, max_keys=2):
        """
        Create a B+ tree with maximum 'max_keys' in each node.
        For this demo, max_keys=2 => a node can hold up to 2 keys before splitting.
        """
        self.max_keys = max_keys
        self.root = Node(is_leaf=True)

    def insert(self, key):
        """
        Insert 'key' into the B+ tree.  We:
          1) Find the leaf where 'key' belongs, insert it.
          2) If the leaf overflows (3 keys > max_keys=2), split it and promote up.
          3) If internal nodes overflow, split them and keep promoting up, possibly creating a new root.
        """
        # Step 1: Find leaf and insert the key
        path = []  # will store (parent_node, index_in_parent) for each descent
        current = self.root
        while not current.is_leaf:
            i = 0
            while i < len(current.keys) and key >= current.keys[i]:
                i += 1
            path.append((current, i))
            current = current.children[i]

        # Insert the key in sorted order into this leaf
        current.keys.append(key)
        current.keys.sort()

        # Check for overflow at the leaf
        if len(current.keys) > self.max_keys:
            promoted = self._split_leaf(current)
        else:
            promoted = None

        # Climb back up the path, splitting internal nodes if needed
        while promoted and path:
            # "promoted" is (promoted_key, new_right_node)
            promoted_key, new_right = promoted
            parent, index_in_parent = path.pop()

            # Insert 'promoted_key' + pointer 'new_right' into the parent
            parent.keys.insert(index_in_parent, promoted_key)
            parent.children.insert(index_in_parent + 1, new_right)

            if len(parent.keys) > self.max_keys:
                # Split the parent
                promoted = self._split_internal(parent)
            else:
                # No further splits needed
                promoted = None

        # If we still have a promotion but no parent => we split the root
        if promoted:
            promoted_key, new_right_node = promoted
            new_root = Node(is_leaf=False)
            new_root.keys = [promoted_key]
            new_root.children = [self.root, new_right_node]
            self.root = new_root

    def _split_leaf(self, leaf):
        """
        Split a leaf node that has (max_keys+1) keys.
        We'll keep the left 'max_keys' in the original leaf,
        and put the remainder in a new leaf. The first key of the new leaf
        is the 'promoted_key' that goes upward.
        """
        split_index = self.max_keys  # e.g. if max_keys=2 and we have 3 keys, left gets 2, right gets 1
        left_keys = leaf.keys[:split_index]
        right_keys = leaf.keys[split_index:]

        # Modify the original leaf to become the "left" leaf
        leaf.keys = left_keys

        # Create a new leaf for the "right" side
        new_leaf = Node(is_leaf=True)
        new_leaf.keys = right_keys

        promoted_key = right_keys[0]  # In a B+ tree, we "copy up" the first key of the new right leaf
        return (promoted_key, new_leaf)

    def _split_internal(self, node):
        """
        Split an internal node that has (max_keys+1) keys => (max_keys+2) children.
        We re-use 'node' as the left node, create a new node for the right side,
        and pick a 'promoted_key' to bubble up.

        In a B+ tree, the internal node's keys are each child's 'lowest' (except for the first).
        So after splitting, we recalc the keys in the left node and the right node from their children.
        """
        total_children = len(node.children)  # e.g. if node had 3 keys => 4 children
        split_index = total_children // 2  # with 4 children, split_index=2 => left=2 children, right=2 children

        left_children = node.children[:split_index]
        right_children = node.children[split_index:]

        # Transform the old 'node' into the left internal node
        node.children = left_children
        node.keys = [child.keys[0] for child in left_children[1:]]  # standard B+ way

        # Create a new right internal node
        new_right = Node(is_leaf=False)
        new_right.children = right_children
        new_right.keys = [child.keys[0] for child in right_children[1:]]

        # The 'promoted_key' is the first key of the new right node's leftmost child
        # (that is, right_children[0].keys[0])
        promoted_key = right_children[0].keys[0]

        return (promoted_key, new_right)

    def visualize(self, filename="bplustree_fixed", view=True):
        """
        Create a Graphviz Digraph of the current tree.
        """
        dot = graphviz.Digraph(comment="Corrected B+ Tree")

        def add_nodes_edges(node):
            # Create label for node
            if node.is_leaf:
                label = f"Leaf\\n{', '.join(node.keys)}"
            else:
                label = f"Internal\\n{', '.join(node.keys)}"
            dot.node(str(node.id), label)

            if not node.is_leaf:
                for child in node.children:
                    add_nodes_edges(child)
                    dot.edge(str(node.id), str(child.id))

        add_nodes_edges(self.root)
        dot.render(filename, view=view, format="png")
        print(f"Graphviz file generated: {filename}.png")


if __name__ == "__main__":
    # The exact insertion order from your example
    insertion_order = ["N", "I", "R", "U", "P", "O", "M"]
    bpt = BPlusTree(max_keys=2)
    for k in insertion_order:
        bpt.insert(k)

    # Visualize to a PNG
    bpt.visualize("bplustree_fixed", view=True)

    print("Done. Generated bplustree_fixed.png with the corrected structure.")
