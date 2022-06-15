# SailPoint Backstage
Custom implementation of the open-source developer dashboard toolkit and framework [Backstage](https://backstage.io), created and maintained by Spotify.

## Prerequisites
1. ```Node``` LTS installation (either version 14 or 16)
* **NOTE**: Currently, *only* version 14 seems to be supported for use with Backstage without encountering errors.
2. ```yarn``` Installation

## Configuring GitHub SSO
1. **Navigate to the [developer settings](https://github.com/settings/developers) on your SailPoint GitHub account.**
2. **Select** ```New OAuth App```.
3. **Fill in the required details.**
* Homepage URL ```https://localhost:3000```
* Authorization callback URL ```https://localhost:7007/api/github/auth```
4. **Copy the client ID and generate and copy the new client secret.**
5. **Store both tokens into their respective environment variables.**
```
AUTH_GITHUB_CLIENT_ID
AUTH_GITHUB_CLIENT_SECRET
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

4. Navigate to the frontend at ```http://localhost:3000``` in the browser if the page does not open in the default browser automatically.
