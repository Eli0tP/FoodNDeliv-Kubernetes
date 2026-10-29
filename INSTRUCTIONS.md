# Instructions d'exécution - CPS2010

Ce guide vous explique étape par étape comment déployer le projet depuis zéro sur votre bureau Windows avec Minikube.

## Prérequis
1. **Docker Desktop** ou **Docker engine** en cours d'exécution.
2. **Minikube** installé.
3. **kubectl** installé.
4. **Helm** installé (pour déployer CloudNativePG).

## Démarrage du cluster Minikube
Ouvrez PowerShell en tant qu'administrateur (ou utilisateur classique si Docker est configuré sans sudo) :
```powershell
minikube start --driver=docker
```

## Tâche 1 : Base de données et Services (Read-Write / Read-Only)

### 1.1 Installer l'opérateur CloudNativePG avec Helm
```powershell
helm repo add cnpg https://cloudnative-pg.github.io/charts
helm repo update
helm upgrade --install cnpg cnpg/cloudnative-pg --namespace cnpg-system --create-namespace --wait
```

### 1.2 Déployer le cluster PostgreSQL (rw/ro)
Appliquez le secret et le cluster (Une ligne à la fois) :
```powershell
cd "C:\Users\eliot\Desktop\UM - Malte\CPS2010\k8s"
kubectl apply -f postgres-secret.yaml
kubectl apply -f postgres-cluster.yaml
```

### 1.3 Construire et déployer les applications Spring Boot
Assurez-vous que votre terminal est configuré pour utiliser le Docker daemon de Minikube :
```powershell
& minikube -p minikube docker-env | Invoke-Expression
```

Compilez l'application en lecture-écriture (RW) avec Maven puis Docker :
```powershell
cd "C:\Users\eliot\Desktop\UM - Malte\CPS2010\foodndeliv-rw"
./mvnw clean package -DskipTests
docker build -t eliotp/foodndeliv-rw:latest .
```

Compilez l'application en lecture seule (RO) de la même manière :
```powershell
cd "C:\Users\eliot\Desktop\UM - Malte\CPS2010\foodndeliv-ro"
./mvnw clean package -DskipTests
docker build -t eliotp/foodndeliv-ro:latest .
```

### 1.4 Déployer les API sur Kubernetes
```powershell
cd "C:\Users\eliot\Desktop\UM - Malte\CPS2010\k8s"
kubectl apply -f apps-deployment.yaml
```
Vérifiez que les applications sont en cours d'exécution :
```powershell
kubectl get pods
kubectl get svc
```
