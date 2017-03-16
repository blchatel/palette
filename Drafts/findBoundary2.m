function [ res ] = findBoundary2( source, dir, l, r )
%findBoundary Find the Cb point 
%   Detailed explanation goes here
lab = source;
for it=1:7
   factor = 0.5 * (l+r);
   lab = source + factor*dir;
   testRGB = lab2rgb(lab);
   
   if outBoundary(testRGB)
       r = factor;
   else
       l = factor;
   end
end
res = source + l * dir;

end

%% Function check if out of boundary
function [ isOut ] = outBoundary( testRGB )
    th = 0.002; % 0.5 / 256;
    isOut = testRGB(1) < -th | testRGB(1) > 1+th | testRGB(2) < -th | testRGB(2) > 1+th | testRGB(3) < -th | testRGB(3) > 1+th ;
end