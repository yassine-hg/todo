const autorizeEndpoint = "http://localhost:8180/realms/todo/protocol/openid-connect/auth";
const tokenEndpoint = "http://localhost:8180/realms/todo/protocol/openid-connect/token";
const clientId = "todo-app";
const redirect_url = "http://localhost";


function generetCodeVerifier(){
  const randomvalue = crypto.getRandomValues(new Uint8Array(32))
  return btoa(String.fromCharCode(...randomvalue)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')
}


async function genereteCodeChallenge(codeVerifier) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(codeVerifier))
  return btoa(String.fromCharCode(...new Uint8Array(digest))).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')
}

document.getElementById("login_link_keycload").addEventListener("click", login);


async function login(){
  const state = generetCodeVerifier();
  const codeVerifier = generetCodeVerifier();
  const codeChallenge = await genereteCodeChallenge(codeVerifier);
  window.sessionStorage.setItem("code_verifier", codeVerifier);
  window.sessionStorage.setItem("state", state);
  const args = new URLSearchParams({
    response_type: "code",
    client_id: clientId,
    code_challenge_method: "S256",
    code_challenge: codeChallenge,
    redirect_uri: redirect_url,
    scope: "openid email profile",
    state: state
  });
  window.location = autorizeEndpoint + "?" + args
}


async function handlecallback() {
  if(window.location.search){

    const  args = new URLSearchParams(window.location.search);
    const code = args.get("code");
    const  state = args.get("state");

    if(state !== sessionStorage.getItem("state")){
      console.log("Erreur");
    }else{
      const code_verifier = sessionStorage.getItem("code_verifier");
      const response = await fetch(tokenEndpoint, {
        method:'POST',
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          code: code, 
          client_id: clientId,
          redirect_uri: redirect_url,
          grant_type: "authorization_code",
          code_verifier: code_verifier,
        })
      });
      const result = await response.json();
      console.log(result);
      if(response.ok){
          document.getElementById("login_link").classList.add("hidden");
          document.getElementById("login_link_keycload").classList.add("hidden");
          document.getElementById("user_info").classList.remove("hidden");
        } else {
          document.getElementById("logout_link").classList.remove("hidden");
          document.getElementById("user_info").classList.add("hidden");
        }
      }
    }

  }  

handlecallback();