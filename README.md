# Kubernetes Deployment

This project is deployed to a local Minikube cluster. All manifests live under `k8s/`,
split into `config/`, `infrastructure/`, `app-services/`, and `ingress/`.

## Repository layout

This system is split across **five separate repositories**, one per service, each with
its own `Dockerfile` and CI pipeline:

- `api-gateway` (this repository) — also holds the Kubernetes manifests (`k8s/`)
- `auth-service`
- `user-service`
- `order-service`
- `payment-service`

To build and deploy the full system, clone all five repositories as **sibling
directories** (i.e. all inside the same parent folder, next to each other):

```
course/
├── api-gateway/
├── auth-service/
├── user-service/
├── order-service/
└── payment-service/
```

The manifests in `k8s/app-services/` reference local image names
(e.g. `payment-service:latest`) that only exist after being built and loaded into
Minikube manually — they are not published to any registry. See "Build and load
application images" below for the exact commands.

## Prerequisites

- Docker Desktop running
- Minikube installed
- kubectl installed

## 1. Start Minikube

```powershell
minikube start --cpus=2 --memory=5000 --driver=docker
minikube addons enable ingress
```

## 2. Build and load application images

From each service's root directory, build the jar, build the Docker image, and load
it into Minikube's internal image store:

```powershell
./gradlew clean bootJar
docker build --no-cache -t <service-name>:latest .
minikube image load <service-name>:latest
```

Example for `payment-service`:

```powershell
cd ../payment-service
./gradlew clean bootJar
docker build --no-cache -t payment-service:latest .
minikube image load payment-service:latest
```

Repeat for `order-service`, `auth-service`, `user-service`, and `api-gateway`.

All five manifests currently pin the `latest` tag with `imagePullPolicy: IfNotPresent`.
Because `latest` is mutable, Kubernetes will keep using whatever image with that tag
is already loaded in Minikube and won't automatically pick up a rebuilt image with the
same tag. After rebuilding an image during development, force the pod to pick up the
new build with:

```powershell
kubectl delete pod -l app=<app-label>
```

or restart the whole rollout:

```powershell
kubectl rollout restart deployment/<deployment-name>
```

## 3. Apply all manifests


Manifests are split into subdirectories, so apply recursively from the `api-gateway`
repository root:

```powershell
kubectl apply -f k8s/ --recursive
```

## 4. Verify the deployment

```powershell
kubectl get pods -w
```

Wait until every pod reaches `1/1 Running`. Then confirm readiness explicitly:

```powershell
kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=180s
kubectl wait --for=condition=ready pod -l app=auth-app --timeout=180s
kubectl wait --for=condition=ready pod -l app=user-app --timeout=180s
kubectl wait --for=condition=ready pod -l app=order-app --timeout=180s
kubectl wait --for=condition=ready pod -l app=payment-app --timeout=180s
```

## 5. Map the Ingress hostname

Get the Minikube IP:

```powershell
minikube ip
```

Add it to `C:\Windows\System32\drivers\etc\hosts` (as Administrator):

```
<minikube-ip> app.local
```

## 6. Start the tunnel

In a **separate** terminal window, keep this running:

```powershell
minikube tunnel
```

## 7. Smoke test

```powershell
curl http://app.local/oauth2/jwks
```

A successful response confirms the Ingress, api-gateway, and auth-service are all
reachable end-to-end.

## Resetting the cluster

To wipe all data and redeploy from a clean state:

```powershell
kubectl delete deployments --all
kubectl delete statefulset system-mongodb
kubectl delete pvc mongodb-storage-system-mongodb-0
kubectl delete pvc postgres-pvc
kubectl apply -f k8s/ --recursive
```

Both PostgreSQL and MongoDB data are stored on PersistentVolumeClaims (`postgres-pvc`,
and `mongodb-storage-system-mongodb-0` auto-created from the StatefulSet's
`volumeClaimTemplates`), so they survive pod restarts but must be explicitly deleted
for a full reset.

## Secrets

`k8s/config/secrets.yaml` contains default credentials for local Minikube use only.
Base64 encoding is not encryption — these values are just obfuscated, not protected.
For any real deployment, create the Secret directly with kubectl and never commit
real credentials to version control:

```powershell
kubectl create secret generic db-secrets --from-literal=postgres-password=<your-password>
```