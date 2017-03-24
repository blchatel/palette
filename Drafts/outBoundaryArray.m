function [res] = outBoundaryArray(testLAB)
    th = 0.002; % 0.5 / 256;
    testRGB = lab2rgb(testLAB);
    res = (testRGB(:, 1) < -th | testRGB(:, 1) > 1 + th | testRGB(:, 2) < -th | testRGB(:, 2) > 1 + th | testRGB(:, 3) < -th | testRGB(:, 3) > 1 + th);
end

