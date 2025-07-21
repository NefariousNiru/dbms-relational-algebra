import matplotlib.pyplot as plt
import networkx as nx
import graphviz

# =====================
# Helper Functions
# =====================

def attribute_closure(attributes, relation, fds):
    """
    Compute the closure of a set of attributes (subset of 'relation')
    given a list of functional dependencies (fds).
    """
    closure = set(attributes)
    changed = True
    while changed:
        changed = False
        for (lhs, rhs) in fds:
            # Only consider the FD if its lhs is a subset of the current closure.
            if lhs.issubset(closure) and not rhs.issubset(closure):
                # Add only attributes that are actually in the current relation.
                new_attrs = rhs.intersection(relation)
                if not new_attrs.issubset(closure):
                    closure.update(new_attrs)
                    changed = True
    return closure


def project_fds(relation, fds):
    """
    Returns the FDs whose left- and right–sides are entirely contained in the given relation.
    """
    proj = []
    for (lhs, rhs) in fds:
        if lhs.issubset(relation) and rhs.issubset(relation):
            proj.append((lhs, rhs))
    return proj


# =====================
# Data Structure for the Decomposition Tree
# =====================

class Node:
    def __init__(self, relation, splitting_fd=None):
        """
        relation: a set of attribute names (e.g. {"sid", "sname", "address"}).
        splitting_fd: The functional dependency (X -> Y) that caused the decomposition of this node.
        children: list to hold child nodes from the decomposition.
        """
        self.relation = relation
        self.splitting_fd = splitting_fd  # (lhs, rhs) tuple that triggered decomposition
        self.children = []

    def __str__(self):
        # Join attributes with commas (this works well with names longer than one character)
        rel_str = ", ".join(sorted(self.relation))
        if self.splitting_fd:
            lhs, rhs = self.splitting_fd
            fd_str = "{} -> {}".format(", ".join(sorted(lhs)), ", ".join(sorted(rhs)))
            return f"{rel_str}  [{fd_str}]"
        else:
            return rel_str


# =====================
# BCNF Decomposition Algorithm
# =====================
def bcnf_decompose(node, fds):
    """
    Recursively decompose the relation (stored in node) into BCNF.
    fds: full list of functional dependencies.
    """
    rel = node.relation
    proj_fds = project_fds(rel, fds)

    # Check each FD to see if it violates BCNF.
    for (lhs, rhs) in proj_fds:
        cl = attribute_closure(lhs, rel, proj_fds)
        if cl != rel:  # FD violates BCNF if lhs is not a superkey.
            # Decompose based on the FD: X -> Y.
            # R1 = X ∪ Y; R2 = R - (Y - X)
            R1 = lhs.union(rhs)
            R2 = rel.difference(rhs.difference(lhs))

            # Record the FD used for splitting at this node.
            node.splitting_fd = (lhs, rhs)
            # Create child nodes for the decomposed relations.
            child1 = Node(R1)
            child2 = Node(R2)
            node.children = [child1, child2]

            # Recursively decompose each child.
            bcnf_decompose(child1, fds)
            bcnf_decompose(child2, fds)
            return  # Decompose once a violation is fixed.
    return


# =====================
# Tree Visualization Functions
# =====================

def visualize_tree_graphviz(root, filename='bcnf_decomposition_tree'):
    """
    Builds a Graphviz Digraph from the decomposition tree and renders it.
    The result is saved to a file (for example, a PDF or PNG).
    """
    dot = graphviz.Digraph(comment='BCNF Decomposition Tree')
    # You can adjust global node attributes if desired.
    dot.attr('node', shape='box', fontsize='12', margin='0.2,0.1')

    # Use a counter to generate unique node IDs.
    node_counter = [0]

    def add_node_rec(node, parent_id=None):
        curr_id = str(node_counter[0])
        node_counter[0] += 1

        # Use the node's string as label; if it has two lines, Graphviz will render them nicely.
        label = str(node)
        dot.node(curr_id, label)

        if parent_id is not None:
            dot.edge(parent_id, curr_id)

        for child in node.children:
            add_node_rec(child, curr_id)

    add_node_rec(root)

    # Render the result to a file and open if possible.
    dot.render(filename, view=True)
    return dot

# =====================
# Example: Using a Relation with Arbitrary Attribute Names
# =====================

# def get_R_fd():
#     # Define a relation with more descriptive attribute names.
#     R = {"C", "T", "G", "H", "R", "S"}
#
#     # Define functional dependencies for the relation.
#     # For example, consider an FD where "sid" functionally determines "sname".
#     fds = [
#         ({"C"}, {"T"}),
#         ({"C", "S"}, {"G"}),
#         ({"H", "R"}, {"C"}),
#         ({"H", "S"}, {"R"}),
#         ({"H", "T"}, {"R"})
#     ]
#     return R, fds

def get_R_fd():
    # Define a relation with more descriptive attribute names.
    R = { "accno", "bname", "cname", "balance", "street", "ccity", "assets", "bcity" }
    # Format: ({LHS} -> {RHS)}
    fds = [
        ({"cname"}, {"street", "ccity"}),
        ({"bname"}, {"assets", "bcity"}),
        ({"acc_no"}, {"bname", "cname", "balance"}),
    ]

    return R, fds

if __name__ == "__main__":
    R, fds = get_R_fd()

    # Create the root node of the decomposition tree.
    root = Node(R)

    # Perform the BCNF decomposition.
    bcnf_decompose(root, fds)


    # Print the decomposition tree.
    def print_tree(node, level=0):
        indent = "    " * level
        print(indent + str(node))
        for child in node.children:
            print_tree(child, level + 1)


    print("BCNF Decomposition Tree:")
    print_tree(root)

    # Build a graph from the tree and visualize it.
    # G = build_graph_from_tree(root)
    # plot_tree(G)

    visualize_tree_graphviz(root, filename="bcnf_decomposition_tree")
