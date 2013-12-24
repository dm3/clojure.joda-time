#!/usr/bin/env sh
lein doc && cd doc \
    && git checkout gh-pages && \
    git add . && git commit -am "Documentation update" \
    && git push -u origin gh-pages && \
    cd ..
