#!/bin/bash

#SBATCH --job-name=algorithm_test
#SBATCH --nodes=1
#SBATCH --tasks-per-node=1
#SBATCH --cpus-per-task=1
#SBATCH --mem=2000
#SBATCH --time=02:00:00
#SBATCH --partition=base
#SBATCH --array=0-2%3

#SBATCH --output=/work_beegfs/sunip987/BatchProcessing/SlurmLogs/%x_%A_%a.out
#SBATCH --error=/work_beegfs/sunip987/BatchProcessing/SlurmLogs/%x_%A_%a.err

#SBATCH --mail-type=END,FAIL
#SBATCH --mail-user=stu222821@mail.uni-kiel.de

module load gcc12-env/12.1.0
module load openjdk/21.0.3_9

cd "$SLURM_SUBMIT_DIR"

# Input files
FILES=(Inputs/Test/*.txt)
INPUT="${FILES[$SLURM_ARRAY_TASK_ID]}"

# Output directory
OUTPUT_DIR="Results/Test"
mkdir -p "$OUTPUT_DIR"

# Input filename without path or .txt
NAME=$(basename "$INPUT" .txt)

echo "Running: $INPUT"
echo "Output directory: $OUTPUT_DIR"

# Run NFold
java -jar ./NFoldAlg.jar "$INPUT" > "$OUTPUT_DIR/${NAME}_NFoldAlg.out"

# Run Lars
java -jar ./LarsAlg.jar "$INPUT" > "$OUTPUT_DIR/${NAME}_LarsAlg.out"

# Run Makeshift
java -jar ./MakeshiftAlg.jar "$INPUT" > "$OUTPUT_DIR/${NAME}_MakeshiftAlg.out"

# Run Steinitz
java -jar ./Steinitz.jar "$INPUT" > "$OUTPUT_DIR/${NAME}_Steinitz.out"

echo "Finished: $INPUT"