# Fonctonnement du serveur JarRet

## Choix d'implémentation
Le serveur se décompose en plusieurs sous parties :
 - Le corps du serveur (Server.java)
 - La définition d'un job (JobDefinition.java)
 - La logique d'un job (Job.java)
 - Le contexte actuel du serveur (Context.java)
 - La tache à effectuer pour ce contexte (TaskContext.java)
 
L'idée est de decouper un maximum la logique du serveur pour obtenir un résultat clair.

## Logique de traitement des clients
Lorsqu'un client se connecte au serveur il est 