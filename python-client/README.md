# portal-backend-client

A lightweight Python client for the portal-backend API.

## Installation

```bash
pip install portal-backend-client
```

## Usage

```python
from portal_backend_client import PortalBackendClient

client = PortalBackendClient("https://portal.example.org")
algorithms = client.get_algorithms()
```

## Jupyter Notebook

There is a ready-to-run notebook in `PortalBackendClient.ipynb` for trying the client.

```bash
cd /home/kfilippopolitis/Desktop/portal-backend/python-client
python -m pip install jupyter
python -m notebook
```

Open `PortalBackendClient.ipynb` and run the cells.

If authentication is enabled, log in via the browser first and copy the `JSESSIONID`
and `XSRF-TOKEN` cookies into the notebook cell that calls
`client.set_session_cookies(...)`.
