import random
import math

def expected_distinct(domain_size: int, sample_size: int) -> float:
    """
    Analytical expectation for the number of distinct keys when
    drawing `sample_size` items uniformly (with replacement)
    from a domain of size `domain_size`.
    """
    if domain_size <= 0:
        return 0.0
    return domain_size * (1 - ((domain_size - 1) / domain_size) ** sample_size)


def monte_carlo_distinct(domain_size: int,
                         sample_size: int,
                         trials: int = 100_000) -> float:
    """
    Monte Carlo estimate of distinct keys for the same setup.
    """
    total = 0
    for _ in range(trials):
        picks = [random.randint(1, domain_size) for _ in range(sample_size)]
        total += len(set(picks))
    return total / trials


if __name__ == "__main__":
    # === User parameters ===
    total_rows = 2000  # e.g. size of your deposit table
    domain_customers = 1000  # distinct customer_names in the full table
    domain_branches = 100  # distinct branches in the full table
    filter_branch = "alps"  # not used in calc, but for context…

    # after filtering on branch you get:
    filtered_rows = total_rows // domain_branches  # e.g. 2000/100 = 20

    # === Compute ===
    analytic = expected_distinct(domain_size=domain_customers,
                                 sample_size=filtered_rows)
    mc = monte_carlo_distinct(domain_size=domain_customers,
                              sample_size=filtered_rows,
                              trials=50_000)

    print(f"Filtered rows (k)           = {filtered_rows}")
    print(f"Domain size (D)             = {domain_customers}")
    print(f"Expected distinct (analytic)= {analytic:.2f}")
    print(f"Expected distinct (MC, n=50k)= {mc:.2f}")
