import pandas as pd
import numpy as np

# Load original
df = pd.read_csv('C:\\Users\\HP\\Documents\\GA_PSO_FEATURE_SELECTION\\data.csv')

# Generate 500 noise features
np.random.seed(42)
n_rows = len(df)
n_noise = 500

noise = np.random.randn(n_rows, n_noise)
noise_df = pd.DataFrame(noise, columns=[f'noise_{i}' for i in range(n_noise)])

# Combine: keep original columns, append noise
combined = pd.concat([df, noise_df], axis=1)

# Save
combined.to_csv('data/dataset_noisy.csv', index=False)

print(f"Original shape: {df.shape}")
print(f"Noisy shape:    {combined.shape}")
print(f"Saved to data/dataset_noisy.csv")