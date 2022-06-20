# SailPoint Backstage
Custom implementation of the open-source developer dashboard toolkit and framework [Backstage](https://backstage.io), created and maintained by Spotify.

## Prerequisites
1. ```Node``` LTS installation (either version 14 or 16)
* **NOTE**: Currently, *only* version 14 seems to be supported for use with Backstage without encountering errors.
2. ```yarn``` Installation

## Configuring GitHub SSO
1. Navigate to the [developer settings](https://github.com/settings/developers) on your SailPoint GitHub account.
2. Open the ```Personal access tokens``` tab and select ```Generate new token```.
3. Fill in your preferred details and select the following scopes from the ```Select scopes``` section:

```
repo
workflow
read:org
read:user
user:email
```

4. Click ```Generate token```.
4. Copy and store the token as an environment variable in your ```~/.bash_profile```.
* ```GITHUB_TOKEN=[your_profile_access_token]```
5. Open the ```OAuth Apps``` tab and select ```New OAuth App```.
6. Fill in the required details.
* Homepage URL ```https://localhost:3000```
* Authorization callback URL ```https://localhost:7007/api/github/auth```
7. Copy the client ID and generate and copy the new client secret. Store both tokens into their respective environment variables in your ```~/.bash_profile```.
```
AUTH_GITHUB_CLIENT_ID=[your_client_id]
AUTH_GITHUB_CLIENT_SECRET=[your_client_secret]
```

## Manual
1. Clone and navigate into the repository.
```
git clone --depth 1 https://github.com/tim-geissler-sp/backstage.git -o [DIRECTORY_NAME]
cd [DIRECTORY_NAME]
```

2. Install the necessary Node modules.
```
yarn install
```

3. Run the frontend and backend concurrently. By default, the frontend is hosted on port ```3000``` and the backend is hosted on port ```7007```.
```
yarn dev
```

## Note for Apple Silicon: 
- Run all commands through a Rosetta 2 terminal with the prefix ```arch -x86_64```,
- Run ```yarn add --dev -W node-gyp``` in the project home folder,
- Make sure the following is present in ```package.json```
```
    "devDependencies": {
        "node-gyp": "^9.0.0"
    },
```
