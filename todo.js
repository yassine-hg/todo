console.log("v2")
const modal = document.getElementById("modal");
const add = document.getElementById("add");
const close_x = document.getElementById("close_x");
const close = document.getElementById("close");

function openModel(){
    modal.classList.remove("hidden");
}
add.addEventListener("click" , openModel)

function closeModel(){
    modal.classList.add("hidden")
}
close_x.addEventListener("click", closeModel);
close.addEventListener("click", closeModel);

function handleOverlayClick(event) {
    if(event.target === modal){
        closeModel()
    }
}

modal.addEventListener("click", handleOverlayClick);


const save = document.getElementById("save");

async function handleSave(){
    const title = document.getElementById("title").value;
    const date = document.getElementById("deadline").value;
     if(title === ""){
        return null;
    }
    await fetch("/api/tasks" , {  //takes two arguments: the URL, and an options object describing the request.
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: "title=" + encodeURIComponent(title) + "&deadline=" + date,
    });

   
    closeModel();
    displaying();
}
save.addEventListener("click" , handleSave);

let taskcontent = document.querySelector(".task_content");


async function displaying(){ // we declare  async so it can  wait before
    const response = await fetch("/api/tasks"); // fetch  data  from  the  port 80 
    const tasks = await response.json();
    taskcontent.innerHTML = "";

    if(tasks.length === 0){
        taskcontent.innerHTML='<p class="no_data">No  data to  diplay</p>';
        return;
    }

    const template = document.getElementById("task_template");
    tasks.forEach(function(task){
        const row = template.content.cloneNode(true);
        row.querySelector(".task_title").textContent = task.title;
        row.querySelector(".task_date").textContent = task.deadline;
        row.querySelector(".task_check").checked = task.done;
        row.querySelector(".task_delete").dataset.id = task.id;
        taskcontent.appendChild(row);
    })
}


taskcontent.addEventListener("click" ,async function(event){
    if(event.target.classList.contains("task_delete")){
        const  idToDelete = Number(event.target.dataset.id);
        await fetch ("/api/tasks/" + idToDelete , {method: "DELETE"})
    
    displaying();
    }
})

displaying();

