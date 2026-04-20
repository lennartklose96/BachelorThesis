import random

# Amount of instances to generate
INSTANCES = 100
# Integer minimum and maximum size per entry in matrix
INT_MIN = 1
INT_MAX = 5
# Minimum and maximum size of each block
BLOCKSIZE_MIN = 8
BLOCKSIZE_MAX = 20
# Objective function limits
C_MIN = -10
C_MAX = 10

# Writing output_file
with open("Datasets/dataset_test.txt", "w") as file:
    # Number of instances on the top of the file
    file.write(f"{INSTANCES}\n")

    ############################# 
    ### GENERATING THE INPUTS ###
    #############################

    # Parameters for generating the instances
    n = 50
    r = 2
    # Generate matrices
    for instance_num in range(INSTANCES):
        # Generate the sizes of the blocks contained in t
        t = [random.randint(BLOCKSIZE_MIN, BLOCKSIZE_MAX) for _ in range(n)]
        h = sum(t)
        A_blocks = []
        for i in range(n):
            A = [[random.randint(INT_MIN, INT_MAX) for _ in range(t[i])] for _ in range(r)]
            A_blocks.append(A)

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

        # Average values for integer and block size
        avg_coeff = (INT_MIN + INT_MAX) / 2
        avg_block = (BLOCKSIZE_MIN + BLOCKSIZE_MAX) / 2
        expected_lhs = avg_coeff * avg_block * n
        # Global and local rhs
        # Global one much more lenient
        rhs_up = [random.randint(0, int(expected_lhs * 2)) for _ in range(r)]
        rhs_down = [random.randint(0, int(avg_coeff * avg_block)) for _ in range(n)]
        # Final RHS = b
        rhs = rhs_up + rhs_down

        # Objective function vector
        c = [random.randint(INT_MIN, INT_MAX) for _ in range(h)]

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