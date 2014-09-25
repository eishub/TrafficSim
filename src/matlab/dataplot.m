fid = fopen(fullfile('..','main','resources','detector_data','detectors.data'));
txt = textscan(fid, '%f%f-%f-%f %f:%f:%f%f%f%f', 'Delimiter', ',');
fclose(fid);
n = size(txt{1},1);
t = zeros(n,1);
for i = 1:n
    % this is much faster than reading the date as string and using datenum
    t(i) = datenum([txt{2}(i) txt{3}(i) txt{4}(i) txt{5}(i) txt{6}(i) txt{7}(i)]);
end

%plots the count data of a specific detector
id = txt{1} == 3189;
times = t(id);
reliability = txt{8}(id);
counts = txt{9}(id);
speeds = txt{10}(id);
ts = timeseries(counts,times);
plot(ts);

%fits a poisson ditricution to the counts and plots its cdf.
%{
fd = fitdist(counts,'Poisson');
fd
low = fd.mean-50;
high = fd.mean+50;
xs = low:1:high;
dist = fd.cdf(xs);
plot(xs, dist);
%}



