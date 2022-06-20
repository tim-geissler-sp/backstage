# [Backstage](https://backstage.io)

This is your newly scaffolded Backstage App, Good Luck!

To start the app, run:

```sh
yarn install
yarn dev
```

Note for Apple Silicon: 
- Run all commands through a Rosetta 2 terminal with the prefix ```arch -x86_64```,
- Run ```yarn add --dev -W node-gyp``` in the project home folder,
- Make sure the following is present in ```package.json```
```
    "devDependencies": {
        "node-gyp": "^9.0.0"
    },
```