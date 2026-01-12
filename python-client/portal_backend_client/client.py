"""HTTP client for interacting with the portal-backend API."""

from __future__ import annotations

from typing import Any, Dict, Optional

import requests


class PortalBackendClient:
    """Client wrapper around the portal-backend REST API."""

    def __init__(
        self,
        base_url: str,
        session: Optional[requests.Session] = None,
        timeout: float = 30.0,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.session = session or requests.Session()
        self.timeout = timeout

    def _url(self, path: str) -> str:
        return f"{self.base_url}{path}"

    def _request(self, method: str, path: str, **kwargs: Any) -> Any:
        kwargs.setdefault("timeout", self.timeout)
        response = self.session.request(method, self._url(path), **kwargs)
        response.raise_for_status()

        if response.status_code == 204:
            return None

        content_type = response.headers.get("Content-Type", "")
        if content_type.startswith("application/json"):
            return response.json()

        return response.text

    def get_algorithms(self) -> Any:
        """GET /algorithms"""
        return self._request("GET", "/algorithms")

    def get_active_user(self) -> Any:
        """GET /activeUser"""
        return self._request("GET", "/activeUser")

    def agree_nda(self) -> Any:
        """POST /activeUser/agreeNDA"""
        return self._request("POST", "/activeUser/agreeNDA")

    def get_data_models(self) -> Any:
        """GET /data-models"""
        return self._request("GET", "/data-models")

    def get_experiments(
        self,
        name: Optional[str] = None,
        algorithm: Optional[str] = None,
        shared: Optional[bool] = None,
        viewed: Optional[bool] = None,
        include_shared: bool = True,
        order_by: str = "created",
        descending: bool = True,
        page: int = 0,
        size: int = 10,
    ) -> Any:
        """GET /experiments"""
        params: Dict[str, Any] = {
            "includeShared": include_shared,
            "orderBy": order_by,
            "descending": descending,
            "page": page,
            "size": size,
        }
        if name is not None:
            params["name"] = name
        if algorithm is not None:
            params["algorithm"] = algorithm
        if shared is not None:
            params["shared"] = shared
        if viewed is not None:
            params["viewed"] = viewed

        return self._request("GET", "/experiments", params=params)

    def get_experiment(self, uuid: str) -> Any:
        """GET /experiments/{uuid}"""
        return self._request("GET", f"/experiments/{uuid}")

    def create_experiment(self, experiment_execution: Dict[str, Any]) -> Any:
        """POST /experiments"""
        return self._request("POST", "/experiments", json=experiment_execution)

    def update_experiment(self, uuid: str, experiment_request: Dict[str, Any]) -> Any:
        """PATCH /experiments/{uuid}"""
        return self._request("PATCH", f"/experiments/{uuid}", json=experiment_request)

    def delete_experiment(self, uuid: str) -> Any:
        """DELETE /experiments/{uuid}"""
        return self._request("DELETE", f"/experiments/{uuid}")

    def create_transient_experiment(self, experiment_execution: Dict[str, Any]) -> Any:
        """POST /experiments/transient"""
        return self._request("POST", "/experiments/transient", json=experiment_execution)
