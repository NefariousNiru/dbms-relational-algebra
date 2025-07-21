# Linear Hashing Simulation with Full Rehashing and Correct Bit Patterns

BUCKET_SIZE = 2  # Maximum number of keys in a bucket's primary area

# Global variables controlling the linear hashing state.
global_d = 0  # Global (minimum) depth d.
split_pointer = 0  # Current split pointer

# Dictionary holding buckets: keys are bucket IDs.
buckets = {}

# ---------------------------
# Bucket Class Definition
# ---------------------------
class Bucket:
    def __init__(self, bucket_id, local_depth, bit_pattern):
        self.id = bucket_id
        self.local_depth = local_depth  # Number of bits used for this bucket.
        self.bit_pattern = bit_pattern  # Bit pattern for display (e.g., "*", "0", "1", "00", etc.)
        self.keys = []  # Primary area (capacity = BUCKET_SIZE).
        self.overflow = []  # Overflow chain (will be rehashed on splits).

    def __str__(self):
        primary = f"keys={self.keys}"
        ovf = f", overflow={self.overflow}" if self.overflow else ""
        return (f"Bucket {self.id}: ld={self.local_depth}, bp={self.bit_pattern}, "
                f"{primary}{ovf}")


# ---------------------------
# Helper Functions
# ---------------------------
def print_state(step):
    print(f"\n-- After Insertion Step: {step} --")
    for bid in sorted(buckets.keys()):
        print(buckets[bid])
    print(f"Global d = {global_d}, Split Pointer = {split_pointer}")
    print("-" * 50)


def to_bit_pattern(num, length):
    """Return a binary string of 'num' padded to 'length' bits."""
    return format(num, f'0{length}b')


def hlow(key, d):
    """Low-resolution hash function: ASCII(key) mod (2^d).
       For d=0, there is only one bucket (bucket 0)."""
    return ord(key) % (1 << d) if d > 0 else 0


def hhigh(key, D):
    """High-resolution hash function: ASCII(key) mod (2^D)."""
    return ord(key) % (1 << D)


def bucket_index(key):
    """
    Decide bucket index for a key using the algorithm:
       i = hlow(key) = ASCII(key) mod (2^global_d)
       if i < split_pointer then i = hhigh(key) = ASCII(key) mod (2^(global_d+1))
    """
    i_low = hlow(key, global_d)
    if i_low < split_pointer:
        return hhigh(key, global_d + 1)
    return i_low


# ---------------------------
# Splitting Function
# ---------------------------
def split_bucket(bucket_id):
    """
    Split the bucket indicated by the split pointer.
    Rehash all keys (from primary and overflow) and recompute bit patterns
    solely from the bucket ID and the new local depth.
    """
    global global_d, split_pointer, buckets

    bucket_to_split = buckets[bucket_id]
    old_ld = bucket_to_split.local_depth
    new_ld = old_ld + 1
    bucket_to_split.local_depth = new_ld

    # Compute the new bit pattern for the bucket being split based on its ID.
    bucket_to_split.bit_pattern = to_bit_pattern(bucket_id, new_ld)

    # Compute the new bucket's id as bucket_id + 2^(old_ld)
    new_bucket_id = bucket_id + (1 << old_ld)
    new_bucket = Bucket(new_bucket_id, new_ld, to_bit_pattern(new_bucket_id, new_ld))
    buckets[new_bucket_id] = new_bucket

    # Gather all keys (primary + overflow) for rehashing.
    all_keys = bucket_to_split.keys + bucket_to_split.overflow
    bucket_to_split.keys = []
    bucket_to_split.overflow = []

    # Redistribute every key using modulus 2^(new_ld)
    for key in all_keys:
        candidate = ord(key) % (1 << new_ld)
        # Compare candidate with parent's id.
        if candidate == new_bucket_id:
            if len(new_bucket.keys) < BUCKET_SIZE:
                new_bucket.keys.append(key)
            else:
                new_bucket.overflow.append(key)
        else:
            if len(bucket_to_split.keys) < BUCKET_SIZE:
                bucket_to_split.keys.append(key)
            else:
                bucket_to_split.overflow.append(key)

    print(f"\nSplitting Bucket {bucket_id} (old ld={old_ld}):")
    print(
        f"   Updated Bucket {bucket_to_split.id} -> bp={bucket_to_split.bit_pattern}, keys: {bucket_to_split.keys}, overflow: {bucket_to_split.overflow}")
    print(
        f"   New Bucket {new_bucket.id} -> bp={new_bucket.bit_pattern}, keys: {new_bucket.keys}, overflow: {new_bucket.overflow}")

    # Advance the split pointer.
    split_pointer += 1
    if split_pointer == (1 << global_d):
        split_pointer = 0
        global_d += 1
        print(f"-> Global d increased to {global_d} and split pointer reset to {split_pointer}.")


# ---------------------------
# Insertion Function
# ---------------------------
def insert_key(key, step):
    global buckets, global_d, split_pointer
    print(f"\nInserting key: {key} (ASCII={ord(key)})")

    candidate = bucket_index(key)

    # Ensure the candidate bucket exists.
    if candidate not in buckets:
        buckets[candidate] = Bucket(candidate, global_d, "*" if global_d == 0 else to_bit_pattern(candidate, global_d))

    if len(buckets[candidate].keys) < BUCKET_SIZE:
        buckets[candidate].keys.append(key)
        print(f"Key {key} inserted into Bucket {candidate} (primary area).")
    else:
        print(f"Bucket {candidate} is full (keys={buckets[candidate].keys}, overflow={buckets[candidate].overflow}).")
        print(f"Splitting bucket {split_pointer} (per split pointer).")
        split_bucket(split_pointer)
        candidate = bucket_index(key)
        if candidate not in buckets:
            buckets[candidate] = Bucket(candidate, global_d,
                                        "*" if global_d == 0 else to_bit_pattern(candidate, global_d))
        if len(buckets[candidate].keys) < BUCKET_SIZE:
            buckets[candidate].keys.append(key)
            print(f"Key {key} inserted into Bucket {candidate} (after split).")
        else:
            buckets[candidate].overflow.append(key)
            print(f"Bucket {candidate} is still full after split; key {key} is stored in overflow chain.")

    print_state(step)


# ---------------------------
# Main Simulation Function
# ---------------------------
def main():
    global buckets, global_d, split_pointer
    buckets = {0: Bucket(0, 0, "*")}
    global_d = 0
    split_pointer = 0

    # Keys to be inserted in order.
    keys = ['N', 'I', "R", "U", "P", "O", "M"]

    print("Character -> ASCII, Octal, Binary")
    for key in keys:
        ascii_val = ord(key)
        oct_val = oct(ascii_val)   # Python's oct() returns a string like '0o120'
        bin_val = format(ascii_val, 'b')
        print(f"  {key}: {ascii_val} , {oct_val} , {bin_val}")
    print("---------------------------------------------------")
    print("Initial")
    step_count = 1
    for key in keys:
        insert_key(key, step_count)
        step_count += 1


if __name__ == "__main__":
    main()
