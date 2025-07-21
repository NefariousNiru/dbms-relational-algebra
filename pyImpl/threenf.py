import itertools


def closure(attributes, fds):
    """
    Given a set of attributes and a list of FDs,
    compute the closure of the attributes with respect to the FDs.
    """
    closure_set = set(attributes)
    while True:
        added = False
        for (lhs, rhs) in fds:
            if lhs.issubset(closure_set) and not rhs.issubset(closure_set):
                closure_set = closure_set.union(rhs)
                added = True
        if not added:
            break
    return closure_set


def split_fds(fds):
    """
    Step 1: Split each FD so that its right-hand side is a singleton.
    """
    print("Step 1: Splitting FDs into single-attribute RHS")
    new_fds = []
    for (lhs, rhs) in fds:
        for attr in rhs:
            new_fd = (lhs, frozenset({attr}))
            new_fds.append(new_fd)
            print(f"  {set(lhs)} -> {{{attr}}}")
    return new_fds


def remove_extraneous_lhs(fds, all_attributes):
    """
    Step 1.2: For each FD with a composite LHS,
    check if any attribute is extraneous.
    """
    print("\nStep 1.2: Eliminating extraneous attributes from LHS (if any)")
    new_fds = []
    for (lhs, rhs) in fds:
        new_lhs = set(lhs)
        if len(lhs) > 1:
            for attr in lhs:
                # Try removing attr from LHS and test if the FD still holds.
                test_lhs = new_lhs - {attr}
                # Use the current set of FDs (not modified FD being tested) to compute closure.
                if rhs.issubset(closure(test_lhs, fds)):
                    print(f"  Attribute '{attr}' is extraneous in FD {new_lhs} -> {set(rhs)}; removing it.")
                    new_lhs = test_lhs
        new_fds.append((frozenset(new_lhs), rhs))
    # In our example, every FD has a single attribute on the LHS, so no change.
    for (lhs, rhs) in new_fds:
        print(f"  {set(lhs)} -> {set(rhs)}")
    return new_fds


def remove_redundant_fds(fds, all_attributes):
    """
    Step 1.3: Remove any FD that is redundant.
    For each FD, temporarily remove it and check if its RHS is still implied.
    """
    print("\nStep 1.3: Removing redundant FDs (if any)")
    minimal_fds = list(fds)
    changed = True
    while changed:
        changed = False
        for fd in minimal_fds.copy():
            temp_fds = minimal_fds.copy()
            temp_fds.remove(fd)
            if fd[1].issubset(closure(fd[0], temp_fds)):
                print(f"  FD {set(fd[0])} -> {set(fd[1])} is redundant; removing it.")
                minimal_fds.remove(fd)
                changed = True
    for (lhs, rhs) in minimal_fds:
        print(f"  {set(lhs)} -> {set(rhs)}")
    return minimal_fds


def merge_fds(fds):
    """
    Step 2: Merge FDs that share the same LHS.
    """
    print("\nStep 2: Merging FDs with the same LHS")
    merged = {}
    for (lhs, rhs) in fds:
        if lhs in merged:
            merged[lhs] = merged[lhs].union(rhs)
        else:
            merged[lhs] = set(rhs)
    for lhs, rhs in merged.items():
        print(f"  {set(lhs)} -> {rhs}")
    return [(lhs, frozenset(rhs)) for lhs, rhs in merged.items()]


def form_tables(fds):
    """
    Step 3: Form a table (relation) for each FD.
    Each table is the union of the LHS and RHS of the FD.
    """
    print("\nStep 3: Forming a table for each FD")
    tables = []
    for (lhs, rhs) in fds:
        table = lhs.union(rhs)
        print(f"  Table: {table}")
        tables.append(table)
    return tables


def remove_subset_tables(tables):
    """
    Step 4: Remove any table that is a subset of another table.
    """
    print("\nStep 4: Removing subset tables (if any)")
    final_tables = tables.copy()
    for t1 in tables:
        for t2 in tables:
            if t1 != t2 and t1.issubset(t2):
                if t1 in final_tables:
                    print(f"  Table {t1} is a subset of {t2}; removing {t1}.")
                    final_tables.remove(t1)
                    break
    return final_tables


def candidate_keys(attributes, fds):
    """
    Compute candidate keys for the relation using a brute-force search.
    Returns a list of candidate keys (each as a set).
    """
    all_attributes = set(attributes)
    keys = []
    for r in range(1, len(all_attributes) + 1):
        for subset in itertools.combinations(all_attributes, r):
            subset_set = set(subset)
            if closure(subset_set, fds) == all_attributes:
                # Check minimality: none of its proper subsets is a candidate key.
                is_minimal = True
                for key in keys:
                    if key.issubset(subset_set):
                        is_minimal = False
                        break
                if is_minimal:
                    keys.append(subset_set)
        if keys:
            break
    return keys


def ensure_losslessness(final_tables, attributes, fds):
    """
    Step 5: Ensure the decomposition is lossless by checking that
    at least one table contains a candidate key.
    If none does, add a table containing a candidate key.
    """
    print("\nStep 5: Ensuring lossless decomposition")
    keys = candidate_keys(attributes, fds)
    print(f"  Candidate key(s) for the relation: {keys}")
    candidate_present = False
    for table in final_tables:
        for key in keys:
            if key.issubset(table):
                candidate_present = True
                print(f"  Table {table} contains the candidate key {key}.")
                break
        if candidate_present:
            break
    if not candidate_present:
        # If no table contains a candidate key, add one.
        new_table = keys[0]
        print(f"  No table contains a candidate key; adding new table: {new_table}")
        final_tables.append(new_table)
    return final_tables


def main():
    attributes = {
        "user_id", "email", "name", "password", "role", "is_active",
        "encrypted_public_key", "encrypted_private_key", "encrypted_key_created_at",
        "auth_failed_attempts", "auth_last_attempt", "blocked_until",
        "document_id", "uploader_id", "owner_id", "encrypted_data", "hash", "document_created_at", "document_title",
        "audit_log_id", "audit_user_id", "action", "timestamp", "ip_address", "user_agent", "audit_document_id",
        "auth_token_id", "token_user_id", "token", "token_created_at", "token_expires_at",
        "access_request_id", "requested_id", "requested_document_id", "status", "access_requested_at",
        "access_approved_at"
    }

    # Define FDs
    fds = [
        # FD1: User Table
        (frozenset({"user_id"}), frozenset({"email", "name", "password", "role", "is_active"})),

        # FD2: Encrypted Key Store Table
        (frozenset({"user_id"}),
         frozenset({"encrypted_public_key", "encrypted_private_key", "encrypted_key_created_at"})),

        # FD3: Auth Log Table
        (frozenset({"user_id"}), frozenset({"auth_failed_attempts", "auth_last_attempt", "blocked_until"})),

        # FD4: Document Table
        (frozenset({"document_id"}),
         frozenset({"uploader_id", "owner_id", "encrypted_data", "hash", "document_created_at", "document_title"})),

        # FD5: Audit Log Table
        (frozenset({"audit_log_id"}),
         frozenset({"audit_user_id", "audit_document_id", "action", "timestamp", "ip_address", "user_agent"})),

        # FD6: Auth Token Table
        (frozenset({"auth_token_id"}), frozenset({"token_user_id", "token", "token_created_at", "token_expires_at"})),

        # FD7: Access Request Table
        (frozenset({"access_request_id"}),
         frozenset({"requested_id", "requested_document_id", "status", "access_requested_at", "access_approved_at"})),
    ]

    print("=== Initial Setup ===")
    print("Attributes:", attributes)
    print("Functional Dependencies (FDs):")
    for lhs, rhs in fds:
        print(f"  {set(lhs)} -> {set(rhs)}")

    # Step 1: Form the minimal cover by splitting FDs into singleton RHS.
    minimal_cover = split_fds(fds)
    print("\nAfter Step 1 (Minimal Cover):")
    for lhs, rhs in minimal_cover:
        print(f"  {set(lhs)} -> {set(rhs)}")

    # Step 2: Eliminate extraneous attributes from LHS (none in our example).
    minimal_cover = remove_extraneous_lhs(minimal_cover, attributes)
    print("\nAfter 1.1 (Extraneous Attributes Eliminated):")
    for lhs, rhs in minimal_cover:
        print(f"  {set(lhs)} -> {set(rhs)}")

    # Step 3: Remove redundant FDs.
    minimal_cover = remove_redundant_fds(minimal_cover, attributes)
    print("\nAfter 1.2 (Redundant FDs Removed):")
    for lhs, rhs in minimal_cover:
        print(f"  {set(lhs)} -> {set(rhs)}")

    # Step 4: Merge FDs with the same LHS.
    merged_fds = merge_fds(minimal_cover)
    print("\nAfter Step 2 (Merged FDs with same LHS):")
    for lhs, rhs in merged_fds:
        print(f"  {set(lhs)} -> {set(rhs)}")

    # Step 5: Form a table for each FD.
    tables = form_tables(merged_fds)
    print("\n2.1 Tables formed from the FDs:")
    for table in tables:
        print(f"  {table}")

    # Step 6: Remove any tables that are a subset of another table.
    final_tables = remove_subset_tables(tables)
    print("\nStep 3 (Subset Tables Removed):")
    for table in final_tables:
        print(f"  {table}")

    # Step 7: Ensure lossless decomposition by including a candidate key.
    final_tables = ensure_losslessness(final_tables, attributes, fds)
    print("\n=== Final 3NF Decomposition Schema ===")
    for idx, table in enumerate(final_tables, start=1):
        print(f"  T{idx}: {table}")


if __name__ == "__main__":
    main()
