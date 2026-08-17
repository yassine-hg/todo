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
let tasks = [];


const save = document.getElementById("save");

function handleSave(){
    const title = document.getElementById("title").value;
    const date = document.getElementById("deadline").value;
    const task={
        id: Date.now(),
        title : title,
        deadline : date,
        done: false
    }

    if(title === ""){
        return null;
    }

    tasks.push(task);
    closeModel();
    displaying();
}
save.addEventListener("click" , handleSave);

let taskcontent = document.querySelector(".task_content");


function displaying(){
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


taskcontent.addEventListener("click" , function(event){
    if(event.target.classList.contains("task_delete")){
        const  idToDelete = Number(event.target.dataset.id);
        tasks = tasks.filter(function(task) {
            return task.id !== idToDelete;
    });
    
    displaying();
    }
})

