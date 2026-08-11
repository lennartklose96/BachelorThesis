import os
import random
from itertools import product

# Generates unique columns based on the input parameters, A, r and n
def generate_unique_columns(a_int_max, r, blocksize):

    possible_columns = (a_int_max + 1) ** r
    # Only enumerate when the space is small
    if possible_columns <= 10000:
        all_columns = list(
            product(
                range(a_int_max + 1),
                repeat=r
            )
        )
        return [list(col) for col in random.sample(all_columns, blocksize)]

    # Otherwise use rejection sampling
    seen = set()
    cols = []

    while len(cols) < blocksize:
        col = tuple(
            random.randint(0, a_int_max)
            for _ in range(r)
        )

        if col not in seen:
            seen.add(col)
            cols.append(list(col))

    return cols

def generate_dataset(
    filename,
    instances,
    a_int_max,
    b_int_max,
    c_max,
    blocksize,
    n,
    r
): 

    # Writing output_file
    with open(filename, "w") as file:
        # Writing the relevant function parameters
        file.write(f"{a_int_max} {b_int_max} {blocksize} {n} {r}\n")
        # Number of instances on the top of the file
        file.write(f"{instances}\n")

        ############################# 
        ### GENERATING THE INPUTS ###
        #############################

        # Generate matrices
        for _ in range(instances):
            # Generate the sizes of the blocks contained in t
            t = [blocksize for _ in range(n)]
            
            ####################
            ### Generating A ###
            ####################
            
            # Making sure there are no duplicate columns
            A_blocks = []
            for block_idx in range(n):
                # Generate unique columns for this block
                cols = generate_unique_columns(
                    a_int_max,
                    r,
                    t[block_idx]
                )
                # Convert columns to block matrix
                block = [[0 for _ in range(t[block_idx])] for _ in range(r)]

                for i in range(r):
                    for j in range(t[block_idx]):
                        block[i][j] = cols[j][i]
                A_blocks.append(block)

            # Width of the matrix
            h = sum(t)
        
            # Upper part of the matrix, global constraints
            global_matrix = []
            for j in range(r):
                row = []
                for i in range(n):
                    row.extend(A_blocks[i][j])
                global_matrix.append(row)

            # Build the local matrix 
            local_matrix = []
            for j in range(n):
                row = []
                for i in range(n):
                    if i == j:
                        row.extend([1] * t[i])
                    else:
                        row.extend([0] * t[i])
                local_matrix.append(row)
            
            # Final matrix A
            matrix = global_matrix + local_matrix      

            ####################
            ### Generating x ###
            ####################
            x_blocks = []
            for i in range(n):
                x_i = [random.randint(0, 5) for _ in range(t[i])]
                x_blocks.append(x_i)

            ####################
            ### Generating b ###
            ####################    

            # Guaranteed feasibility
            if random.random() < 0.5:
                rhs_down = [sum(x_blocks[i]) for i in range(n)]
                rhs_up = []
                for k in range(r):
                    total = 0
                    for i in range(n):
                        for j in range(t[i]):
                            total += A_blocks[i][k][j] * x_blocks[i][j]
                    rhs_up.append(total)
                # Final RHS = b
                rhs = rhs_up + rhs_down
            # Entirely random rhs, unlikely to be feasible
            else:
                rhs = [random.randint(0, b_int_max) for _ in range(r + n)]


            # Objective function vector
            c = [random.randint(0, c_max) for _ in range(h)]

            ####################################
            ### WRITING INSTANCE INFORMATION ###
            ####################################


    
            # Writing the input constraints
            file.write(f"{n} {r} {h}\n")
            # Writing t, c and rhs
            file.write(" ".join(map(str, t)) + "\n")
            file.write(" ".join(map(str, c)) + "\n")
            file.write(" ".join(map(str, rhs)) + "\n")
            # Writing the matrix
            for row in matrix:
                file.write(" ".join(map(str, row)) + "\n")
            # New line to seperate instances
            file.write("\n")


def generate_cluster_inputs():
    # Input paremeter ranges to test
    a_ranges = [(1, 10, 1), (20, 100, 20), (500, 1000, 500)]
    b_ranges = [(1, 10, 1), (20, 100, 20), (500, 1000, 500)]
    block_ranges = [(2, 10, 2)]
    r_ranges = [(1, 10, 1)]
    # Turns the range values into a list
    def generate_range(ranges):
        values = []
        for start, end, step in ranges:
            values.extend(range(start, end + 1, step))
        return sorted(set(values))
    # Extracting the values from the ranges
    a_values = generate_range(a_ranges)
    b_values = generate_range(b_ranges)
    block_values = generate_range(block_ranges)
    r_values = generate_range(r_ranges)


    dataset_id = 0
    c_max = 10
    # Generate a lot of datasets
    for a_max, b_max, block_size, r in product(
            a_values,
            b_values,
            block_values,
            r_values):
        
        dataset_id += 1

        # Impossible dataset, ignore this usecase
        possible_columns = (a_max + 1) ** r
        if possible_columns >= block_size:
            # Find the correct folder to process
            r_folder = f"Inputs/R{r}"

            os.makedirs(r_folder, exist_ok=True)
            filename = os.path.join(
                r_folder,
                f"A{a_max}_B{b_max}_Block{block_size}_R{r}.txt"
            )
            generate_dataset(filename, 10, a_max, b_max, c_max, block_size, block_size, r)
            print(f"Generated {dataset_id} dataset")





#########################################
### Generate a random testing dataset ###
#########################################

# Amount of instances to generate
INSTANCES = 10
# Integer minimum and maximum size per entry in matrix
a_int_max = 3
# Integer minimum and maximum size per entry in RHS
b_int_max = 3
# Minimum and maximum size of each block
blocksize = 3
blocknum = 3
# Objective function limits
c_max = 9
# Amount of rows in the upper matrix
r = 1
generate_dataset("Datasets/dataset_test.txt", INSTANCES, a_int_max,
                b_int_max,  c_max, blocksize, blocknum, r)

generate_cluster_inputs()