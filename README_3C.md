# Fascicolo Sanitario 2.0

# _it-fse-gtw-dispatcher_

Fork del repository [ministero-salute/it-fse-gtw-dispatcher](https://github.com/ministero-salute/it-fse-gtw-dispatcher.git).

---

Il repository contiene modifiche necessarie al funzionamento del Gateway FSE 2.0 nell'ambiente del private cloud HTN.

Si è scelto di mantenere nel branch `stable` le modifiche necessarie al funzionamento del Gateway FSE 2.0 nell'ambiente del private cloud HTN, mentre nel branch `main` si mantiene il codice del repository originale.

## Procedura di allineamento

Per mantenere il fork allineato al repository originale è necessario seguire la seguente procedura:

1. Se non presente, impostare il repository originale come `upstream`:

```bash
git remote add upstream https://github.com/ministero-salute/it-fse-gtw-dispatcher.git
```

2. Scaricare le modifiche dal repository originale:

```bash
git fetch upstream
git checkout main
git merge upstream/main
```

3. Effettuare il rebase delle modifiche del repository originale sul branch `stable`:

```bash
git checkout stable
git rebase main
```

4. Risolvere eventuali conflitti e fare il push sul repository fork:

```bash
git push --force-with-lease origin
```

5. Se necessario, aggiornare il tag della versione:

```bash
git tag -a <versione>
git push origin <versione>
```

