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
