import random

# Amount of instances to generate
INSTANCES = 100
# Integer minimum and maximum size per entry in matrix
A_INT_MIN = 0
A_INT_MAX = 3
# Integer minimum and maximum size per entry in RHS
B_INT_MIN = 2
B_INT_MAX = 5
# Minimum and maximum size of each block
BLOCKSIZE = 3
# Objective function limits
C_MIN = 0
C_MAX = 0

# Writing output_file
with open("Datasets/dataset_stein.txt", "w") as file:
    # Number of instances on the top of the file
    file.write(f"{INSTANCES}\n")

    ############################# 
    ### GENERATING THE INPUTS ###
    #############################

    # Parameters for generating the instances
    n = 3
    r = 1
    # Generate matrices
    for instance_num in range(INSTANCES):
        # Generate the sizes of the blocks contained in t
        t = [BLOCKSIZE for _ in range(n)]
        
        ####################
        ### Generating A ###
        ####################
        
        # Making sure there are no duplicate columns
        A_blocks = []
        for block_idx in range(n):
            seen = set()
            cols = []
            iterations = t[block_idx]
            # Generate columns
            count = 0
            while count < iterations:
                # Generate one column
                col = tuple(random.randint(A_INT_MIN, A_INT_MAX) for _ in range(r))
                # Avoid duplicates inside the block
                # Duplicates in columns between blocks are permitted as the local part (B) will differ
                if col not in seen:
                    seen.add(col)
                    cols.append(list(col))
                    count += 1
            # Convert columns to blocks
            block = [[0 for _ in range(t[block_idx])] for _ in range(r)]
            for i in range(len(block)):
                for j in range(len(block[0])):
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
            x_i = [random.randint(0, 2) for _ in range(t[i])]
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
            rhs = [random.randint(B_INT_MIN, B_INT_MAX) for _ in range(r + n)]


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