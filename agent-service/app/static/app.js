const form = document.querySelector('#chat-form');
const input = document.querySelector('#message-input');
const list = document.querySelector('#messages');
const intro = document.querySelector('#intro');
const send = document.querySelector('#send-button');
let conversationId = sessionStorage.getItem('supportConversation') || null;
let demoAccessCode = sessionStorage.getItem('supportDemoAccessCode') || null;
let demoAccessCodeRequired = false;
const appConfigReady = fetch('/health').then(response => response.json()).then(config => {
  demoAccessCodeRequired = Boolean(config.demo_access_code_required);
}).catch(() => {});

function addMessage(text, role) {
  const row = document.createElement('div');
  row.className = `message ${role}`;
  if (role === 'assistant') {
    const avatar = document.createElement('div'); avatar.className = 'avatar'; avatar.textContent = '✦';
    row.append(avatar);
  }
  const bubble = document.createElement('div'); bubble.className = 'bubble'; bubble.textContent = text;
  row.append(bubble); list.append(row); row.scrollIntoView({behavior: 'smooth', block: 'nearest'});
  return row;
}

async function submitMessage(text) {
  text = text.trim(); if (!text || send.disabled) return;
  await appConfigReady;
  if (demoAccessCodeRequired && !demoAccessCode) {
    demoAccessCode = window.prompt('Enter the demo access code provided by the project owner:');
    if (!demoAccessCode) return;
    sessionStorage.setItem('supportDemoAccessCode', demoAccessCode);
  }
  intro.hidden = true;
  addMessage(text, 'user'); input.value = ''; input.style.height = 'auto'; send.disabled = true;
  const row = addMessage('', 'assistant');
  row.querySelector('.bubble').innerHTML = '<span class="typing" aria-label="Thinking"><i></i><i></i><i></i></span>';
  try {
    const response = await fetch('/api/chat', {
      method: 'POST', headers: {
        'Content-Type': 'application/json',
        ...(demoAccessCodeRequired ? {'X-Demo-Access-Code': demoAccessCode} : {})
      },
      body: JSON.stringify({message: text, conversation_id: conversationId})
    });
    const data = await response.json();
    if (response.status === 401) {
      sessionStorage.removeItem('supportDemoAccessCode'); demoAccessCode = null;
      throw new Error('That access code was not accepted. Please try again.');
    }
    if (!response.ok) throw new Error(data.detail || 'Something went wrong. Please try again.');
    conversationId = data.conversation_id; sessionStorage.setItem('supportConversation', conversationId);
    row.querySelector('.bubble').textContent = data.answer;
  } catch (error) {
    row.querySelector('.bubble').textContent = error.message;
  } finally { send.disabled = false; input.focus(); }
}

form.addEventListener('submit', event => {event.preventDefault(); submitMessage(input.value);});
input.addEventListener('input', () => {input.style.height = 'auto'; input.style.height = `${Math.min(input.scrollHeight, 140)}px`;});
input.addEventListener('keydown', event => {if (event.key === 'Enter' && !event.shiftKey) {event.preventDefault(); form.requestSubmit();}});
document.querySelectorAll('.suggestion').forEach(button => button.addEventListener('click', () => submitMessage(button.dataset.prompt)));
document.querySelector('#new-chat').addEventListener('click', () => {
  conversationId = null; sessionStorage.removeItem('supportConversation'); list.replaceChildren(); intro.hidden = false; input.focus();
});
