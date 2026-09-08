#!/bin/bash

#SBATCH --job-name=r6_processing
#SBATCH --nodes=1
#SBATCH --tasks-per-node=1
#SBATCH --cpus-per-task=1
#SBATCH --mem=2000
#SBATCH --time=05:00:00
#SBATCH --partition=base
#SBATCH --array=0-1104%100

#SBATCH --output=/work_beegfs/sunip987/BatchProcessing/SlurmLogs/%x_%A_%a.out
#SBATCH --error=/work_beegfs/sunip987/BatchProcessing/SlurmLogs/%x_%A_%a.err

#SBATCH --mail-type=END,FAIL
#SBATCH --mail-user=stu222821@mail.uni-kiel.de

module load gcc12-env/12.1.0
module load openjdk/21.0.3_9

cd "$SLURM_SUBMIT_DIR"

# Input files
FILES=(Inputs/R6/*.txt)
INPUT="${FILES[$SLURM_ARRAY_TASK_ID]}"

# Output directory
OUTPUT_DIR="Results/R6"
mkdir -p "$OUTPUT_DIR"

# Input filename without path or .txt
NAME=$(basename "$INPUT" .txt)

echo "========================================"
echo "Running: $INPUT"
echo "Output directory: $OUTPUT_DIR"
echo "========================================"


# ============================================================
# NFold
# ============================================================

echo "Starting NFold..."
START_TIME=$(date +%s%N)

java -jar ./NFoldAlg.jar "$INPUT" > "$OUTPUT_DIR/${NAME}_NFoldAlg.out" 2>&1

EXIT_CODE=$?

END_TIME=$(date +%s%N)
ELAPSED_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo "NFold finished"
echo "NFold elapsed time: ${ELAPSED_MS} ms"

if [ $EXIT_CODE -ne 0 ]; then
    echo "NFold FAILED with exit code $EXIT_CODE"
fi


# ============================================================
# Lars
# ============================================================

echo "Starting Lars..."
START_TIME=$(date +%s%N)

java -jar ./LarsAlg.jar "$INPUT" > "$OUTPUT_DIR/${NAME}_LarsAlg.out" 2>&1

EXIT_CODE=$?

END_TIME=$(date +%s%N)
ELAPSED_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo "Lars finished"
echo "Lars elapsed time: ${ELAPSED_MS} ms"

if [ $EXIT_CODE -ne 0 ]; then
    echo "Lars FAILED with exit code $EXIT_CODE"
fi


# ============================================================
# Makeshift
# ============================================================

echo "Starting Makeshift..."
START_TIME=$(date +%s%N)

java -jar ./MakeshiftAlg.jar "$INPUT" > "$OUTPUT_DIR/${NAME}_MakeshiftAlg.out" 2>&1

EXIT_CODE=$?

END_TIME=$(date +%s%N)
ELAPSED_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo "Makeshift finished"
echo "Makeshift elapsed time: ${ELAPSED_MS} ms"

if [ $EXIT_CODE -ne 0 ]; then
    echo "Makeshift FAILED with exit code $EXIT_CODE"
fi


# ============================================================
# Steinitz
# ============================================================

echo "Starting Steinitz..."
START_TIME=$(date +%s%N)

java -jar ./Steinitz.jar "$INPUT" > "$OUTPUT_DIR/${NAME}_Steinitz.out" 2>&1

EXIT_CODE=$?

END_TIME=$(date +%s%N)
ELAPSED_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo "Steinitz finished"
echo "Steinitz elapsed time: ${ELAPSED_MS} ms"

if [ $EXIT_CODE -ne 0 ]; then
    echo "Steinitz FAILED with exit code $EXIT_CODE"
fi

# ============================================================
# Finished
# ============================================================

echo "========================================"
echo "Finished: $INPUT"
echo "========================================"