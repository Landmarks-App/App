# Detecting Landmarks using Cloud!

Distributed system for submitting and executing cloud computing tasks, with the ability to adapt to load variations (elasticity), with the intent of processing images to detect and verify the existence of monuments or famous landmarks, using integrated services from the Google Cloud Platform, for storage, communication, and computation.

1 - Upload an image, which is stored in Google's Cloud Storage
2 - The image is processed by Google's Vision API
3 - The landmarks detected within the image are stored in Google's Firestore, with their associated result fidelity 
4 - The application generates static maps for each landmark detected using Google's Static Map API
5 - The static maps are stored in Google's Cloud Storage, and the Firestore is updated with their reference link for quick access
