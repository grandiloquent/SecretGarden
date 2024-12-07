const searchQuery = document.querySelector('.search-query');
searchQuery.addEventListener('keydown', evt => {
    if (evt.keyCode === 13) {
        videoWithContextRenderer.innerHTML = '';
        mOffset = 0;
        mSearch = searchQuery.value;
        render()
    }
})



const clearButton = document.querySelector('.clear-button');
clearButton.addEventListener('click', evt => {
    evt.stopImmediatePropagation();
    searchQuery.value = ''
})

const searchButton = document.querySelector('.search-button');
searchButton.addEventListener('click', evt => {
    evt.preventDefault();
    evt.stopImmediatePropagation();
    videoWithContextRenderer.innerHTML = '';
    mOffset = 0;
    mSearch = searchQuery.value;
    render()
})