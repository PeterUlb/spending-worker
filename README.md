# Spending Worker
Processes spending events and is able to return an accumulated count.

## Requirements

### Components
- Postgres Database
   - Either run locally on your PC or on e.g minikube via skaffold (example yaml below)
- PubSub Cluster

### Secrets
The following secrets must be provided to run the application on k8s

#### GCP PubSub Key

**Name:** pubsub-key  
**Value:** key.json=BASE64_FILE_CONTENT  
**Example:**
<details>

`kubectl create secret generic pubsub-key --from-file=key.json=PATH-TO-KEY-FILE.json`
</details>

#### Secrets Configuration

**Name:** spending-worker  
**Values:**  
relevant.property.x=BASE64  
other.property.set=BASE64  
**Example:**
<details>

```
apiVersion: v1
data:
  spring.datasource.password: bGV0bWVpbg==
  spring.datasource.username: ZGV2
kind: Secret
metadata:
  name: spending-worker
```

`kubectl create secret generic spending-worker --from-literal=spring.datasource.user=username --from-literal=spring.datasource.password=p455w0rd`
</details>






# Example YAML

## Postgres
<details>

```
apiVersion: v1
kind: PersistentVolume
metadata:
  name: postgres-pv
  labels:
    type: local
spec:
  storageClassName: manual
  capacity:
    storage: 1Gi
  accessModes:
    - ReadWriteOnce
  hostPath:
    path: /mnt1/postgres-data
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  labels:
    type: local
spec:
  storageClassName: manual
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 500Mi
  volumeName: postgres-pv
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres-statefulset
spec:
  serviceName: "postgres"
  replicas: 1
  selector:
    matchLabels:
      app: postgres # has to match .spec.template.metadata.labels
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:12
          envFrom:
            - configMapRef:
                name: postgres-configuration
          env:
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-credentials
                  key: password
          ports:
            - containerPort: 5432
              name: postgresdb
          volumeMounts:
            - name: postgres-volume-mount
              mountPath: /var/lib/postgresql/data
          readinessProbe:
            exec:
              command:
                - bash
                - "-c"
                - "psql -U$POSTGRES_USER -d$POSTGRES_DB -c 'SELECT 1'"
            initialDelaySeconds: 15
            timeoutSeconds: 2
          livenessProbe:
            exec:
              command:
                - bash
                - "-c"
                - "psql -U$POSTGRES_USER -d$POSTGRES_DB -c 'SELECT 1'"
            initialDelaySeconds: 15
            timeoutSeconds: 2
      volumes:
        - name: postgres-volume-mount
          persistentVolumeClaim:
            claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
spec:
  ports:
    - port: 5432
      name: postgres
  type: NodePort
  selector:
    app: postgres
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-configuration
data:
  POSTGRES_DB: "spending-db"
  POSTGRES_USER: "postgres"
```
</details>

## Postgres Secrets
<details>

```
apiVersion: v1
kind: Secret
metadata:
  name: postgres-credentials
type: Opaque
data:
  password: BASE64PW
```

</details>

## Roles
<details>

```
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
namespace: default
name: namespace-reader
rules:
- apiGroups: [ "", "extensions", "apps" ]
  resources: [ "configmaps", "pods", "services", "endpoints", "secrets" ]
  verbs: [ "get", "list", "watch" ]

---

kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
name: namespace-reader-binding
namespace: default
subjects:
- kind: ServiceAccount
  name: default
  apiGroup: ""
  roleRef:
  kind: Role
  name: namespace-reader
  apiGroup: ""
```
</details>