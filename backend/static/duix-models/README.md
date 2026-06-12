Duix model zips are not committed to git because they are too large for GitHub and too large for the Android base app.

Upload these files to the production model host:

- gj_dh_res.zip
- Sofia.zip
- Oliver.zip
- Lily.zip

The app downloads through:

https://prezzence-backend.onrender.com/api/duix/models/download/{ModelName}.zip

If the files are hosted elsewhere, set DUIX_MODEL_BASE_URL to that public base URL. The backend will redirect the app to that storage URL without requiring an app rebuild.
