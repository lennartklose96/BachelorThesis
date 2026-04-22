import random

# Amount of instances to generate
INSTANCES = 100
# Integer minimum and maximum size per entry in matrix
A_INT_MIN = 0
A_INT_MAX = 10
# Integer minimum and maximum size per entry in x
X_INT_MIN = 0
X_INT_MAX = 2000
# Minimum and maximum size of each block
BLOCKSIZE_MIN = 20
BLOCKSIZE_MAX = 50
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

        ####################
        ### Generating A ###
        ####################
        
        A_blocks = []
        for i in range(n):
            A = [[random.randint(A_INT_MIN, A_INT_MAX) for _ in range(t[i])] for _ in range(r)]
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

        ####################
        ### Generating x ###
        ####################
        x_blocks = []
        for i in range(n):
            x_i = [random.randint(X_INT_MIN, X_INT_MAX) for _ in range(t[i])]
            x_blocks.append(x_i)

        ####################
        ### Generating b ###
        ####################    
       
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

        #############################################
        # TODO: BREAK INSTANCE! All are feasible atm!
        #############################################

        # Objective function vector
        c = [random.randint(C_MIN, C_MAX) for _ in range(h)]

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