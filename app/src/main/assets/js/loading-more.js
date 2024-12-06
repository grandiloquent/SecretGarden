const intersectionObserver = new IntersectionObserver(entries => {
    if (entries[0].intersectionRatio <= 0) return;
    // load more content;
    render();
  });
  // start observing
  intersectionObserver.observe(document.querySelector(".more"));