import random

# Amount of instances to generate
INSTANCES = 100
# Integer minimum and maximum size per entry in matrix
INT_MIN = 0
INT_MAX = 10
# Minimum and maximum size of each block
BLOCKSIZE_MIN = 3
BLOCKSIZE_MAX = 10
# Objective function limits
C_MIN = -10
C_MAX = 10

# Writing output_file
with open("dataset_new.txt", "w") as file:
    # Number of instances on the top of the file
    file.write(f"{INSTANCES}\n")

    ############################# 
    ### GENERATING THE INPUTS ###
    #############################

    # Parameters for generating the instances
    n = 20
    r = 5
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
        # Right hand side
        rhs = [random.randint(INT_MIN, INT_MAX) for _ in range(r+n)]
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