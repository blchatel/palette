%constant used
palette_size = 1; 

% Load images
img = imread('images/bridge.png');

nrows = size(img,1);
ncols = size(img,2);

img = img(1:nrows,1:ncols, :);

nrows = size(img,1);
ncols = size(img,2);

% convert into La*b* domain
lab_img = rgb2lab(img); 

% convert into 3D data point 
data_img = reshape(lab_img, nrows*ncols, 3);
[numInst,numDims] = size(data_img);

%# K-means clustering
%# (K: number of clusters, G: assigned groups, C: cluster centers)
K = palette_size + 1;
[G,C] = kmeans(data_img, K, 'distance','sqEuclidean', 'start','sample');

% sort C by luminance the lighter at the beginning and darker at the end
sortedC = sortrows(C, 1);

rgbPalette = lab2rgb(sortedC)*255;

figure(1);
imshow(img);

figure(2);
for k=2:K

    color = zeros(200, 200, 3);
    
    color(:,:,1) = zeros(200) + rgbPalette(k, 1);
    color(:,:,2) = zeros(200) + rgbPalette(k, 2);
    color(:,:,3) = zeros(200) + rgbPalette(k, 3);
    
    subplot(1,K-1,k-1);
    imshow(uint8(color));
    
end


%# show points and clusters (color-coded)
%clr = lines(K);
%figure, hold on
%scatter3(X(:,1), X(:,2), X(:,3), 36, clr(G,:), 'Marker','.')
%scatter3(C(:,1), C(:,2), C(:,3), 100, clr, 'Marker','o', 'LineWidth',3)
%hold off
%view(3), axis vis3d, box on, rotate3d on
%xlabel('x'), ylabel('y'), zlabel('z')


% imagine we want to change one of the palette's color too a
% redish color (200, 0, 0)
newRGB = [0.7,0.5,0.5];
newLAB = rgb2lab(newRGB);
P = K-1;
palette = sortedC(2:K, :);

i = 1; % The index of the color to change

%% Here we transfer the Luminance first in the Palette
oldPalette = palette;
delta = palette(i, 1) - newLAB(1);
oldPaletteLAB = palette(i, :);

% Update the palette's color
for j=1:P
    if (j ~= i)
        if palette(j, 1) < palette(i, 1)
            palette(j, 1) = newLAB(1) - smoothL(delta, palette(i, 1)-palette(j, 1));
        else
            palette(j, 1) = newLAB(1) + smoothL(-delta, palette(j, 1)-palette(i, 1));
        end
    end
end

% Update the chosen color
palette(i, 1) = newLAB(1);
palette(i, 2) = newLAB(2);
palette(i, 3) = newLAB(3);

newPaletteLAB = palette(i, :);
diff = newPaletteLAB - oldPaletteLAB;

rgbPalette = lab2rgb(palette)*255;
% To be in boundaries
for j=1:P
    rgbPalette(j, 1) = min( 255, max(rgbPalette(j, 1), 0));
    rgbPalette(j, 2) = min( 255, max(rgbPalette(j, 2), 0));
    rgbPalette(j, 3) = min( 255, max(rgbPalette(j, 3), 0));
end

figure(3);
for k=1:P
    
    color = zeros(200, 200, 3);
    
    color(:,:,1) = zeros(200) + rgbPalette(k, 1);
    color(:,:,2) = zeros(200) + rgbPalette(k, 2);
    color(:,:,3) = zeros(200) + rgbPalette(k, 3);
    
    subplot(1,P,k);
    imshow(uint8(color));
    
end

%% Now we transfer to every pixel
% We go on the picture to apply the transfer
image = zeros(nrows, ncols, 3);
Cb = findBoundary2(oldPaletteLAB, oldPaletteLAB + 5 * diff);
C_rate = labDistance(oldPaletteLAB, newPaletteLAB) / labDistance(oldPaletteLAB, Cb);



[p0, p1] = meshgrid(1:nrows, 1:ncols);
pairs = [p0(:), p1(:)];

% tic
% Cb = findBoundary2(oldPalette(i, :), diff, 0, 5);
% toc
% 
% tic
% a = lab2rgb(Cb)
% toc
% 
% toc
% b = rgb2lab(a)
% tic

G = 12;
G_2 = G ^ 2;
[g0, g1, g2] = meshgrid(1:G, 1:G, 1:G);
grid_img = ([g2(:), g0(:), g1(:)] - 1) / (G - 1);
[gm0, gm1, gm2] = meshgrid(0:G-1, 0:G-1, 0:G-1);
gm = gm2(:) * G_2 + gm0(:) * G + gm1(:) + 1;
grid_lab = rgb2lab(grid_img);

% ngm = size(gm, 1);
% for iter = 1:ngm
%     iter
%     index = gm(iter);
%     Lab = grid_lab(index, :);
%     if Lab == oldPaletteLAB
%         newLab = newPaletteLAB;
%     else
%         xb = findBoundary2(Lab, diff, 0, 10);
%         newLab = Lab + (xb - Lab) * C_rate;
%     end
%     rgb = lab2rgb(newLab);
%     rgb = min(1, max(0, rgb));
%     grid_img(index, :) = rgb;
% end

select_old = (grid_lab == oldPaletteLAB);
select_old = select_old(:, 1);
if size(find(select_old), 1) > 0
    grid_img(select_old, :) = zeros(size(find(select_old), 1), 3) + lab2rgb(newPaletteLAB);
end
select_new = find(~select_old);
grid_new = grid_lab(select_new, :);

grid_new_l = grid_new;
grid_new_r = grid_new_l + 10 * diff;
while size(find(outBoundaryArray(grid_new_r)), 1) < size(grid_new_r, 1)
    grid_new_r = grid_new_r + 10 * diff;
end

one_diff_out = outBoundaryArray(grid_new + diff);
grid_new_l(one_diff_out, :) = repmat(newPaletteLAB, size(find(one_diff_out), 1), 1);
grid_new_r(one_diff_out, :) = grid_new(one_diff_out, :) + diff;

grid_new_l = findBoundary2(grid_new_l, grid_new_r)

% grid_new_res = grid_new + (grid_new_l - grid_new) * C_rate;
grid_new_res = findNewColor(grid_new, grid_new_l, C_rate);

grid_new_res = lab2rgb(grid_new_res);
grid_new_res = min(1, max(0, grid_new_res));

grid_img(select_new, :) = grid_new_res;

[nei0, nei1, nei2] = meshgrid(0:1, 0:1, 0:1);
neighbor_color = [nei2(:), nei0(:), nei1(:)];

for iter=1:nrows * ncols
    w = pairs(iter, 1);
    h = pairs(iter, 2);
    if h == 1 && mod(w, 10) == 0
        [w, nrows, ncols]
    end
    orgb = double(reshape(img(w, h, :), 1, 3)) / 255;
    rgb = orgb * (G - 1);
    index = min(floor(rgb), G-2);
    rate = rgb - index;
    rate = repmat(rate, 8, 1);
    rate2 = rate .* (neighbor_color - 0.5) * 2 + 1 - neighbor_color;
    rate2 = repmat(prod(rate2, 2), 1, 3);
    index = index(1) * G_2 + index(2) * G + index(3) + 1;
    indexes = neighbor_color(:, 1) * G_2 + neighbor_color(:, 2) * G + neighbor_color(:, 3) + index;
    colors = grid_img(indexes, :);
    rgb = sum(colors .* rate2, 1);

    image(w, h, :) = reshape(rgb, 1, 1, 3);
end

figure(4)
imshow(image)

%% Function that find a point in the segment with left and right limit given and a factor
function [ inter ] = interpolate( l, r, factor)
    inter = l * factor + r * (1-factor);
end

