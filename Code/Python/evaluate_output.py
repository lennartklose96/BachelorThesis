import pandas as pd
from pathlib import Path
import re

# Output data
data = []

folder = Path("RawResults")

for file_path in folder.rglob("*"):
    if file_path.is_file():
        # Open all files
        with open(file_path, "r") as f:
            lines = f.readlines()
            # Check for empty file
            if not lines:
                print("File is empty")
                continue
            # Getting the algorithm used
            algo = lines[0].strip()
            # Getting parameters
            param_line = lines[2].strip()
            params = [int(x) for x in param_line.strip("[]").split(",")]
            # Check if the file did not time out
            elapsed = None
            if len(lines) > 3:
                match = re.fullmatch(r"Time elapsed: (\d+)", lines[3].strip())
                if match:
                    elapsed = int(match.group(1))
            # Appending to final data
            data.append({
                "algorithm": algo,
                "max_a": params[0],
                "max_b": params[1],
                "block_size": params[2],
                "block_amount": params[3],
                "r": params[4],
                "elapsed": elapsed
            })

# Create data frame
df = pd.DataFrame(data)
df.to_csv("results.csv", index = False)
print("csv created")
                
            